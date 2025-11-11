package com.rentals.eliterentals

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response

class ChatbotTenantActivity : AppCompatActivity() {

    private lateinit var rv: RecyclerView
    private lateinit var adapter: ChatAdapter
    private lateinit var input: EditText
    private lateinit var send: ImageButton
    private lateinit var quickGroup: LinearLayout

    private val thread = mutableListOf<MessageDto>()
    private var failStreak = 0

    private val api by lazy { RetrofitClient.instance }
    private val jwt by lazy { SharedPrefs.getToken(this).orEmpty() }
    private val meId by lazy { SharedPrefs.getUserId(this) }
    private val lang by lazy {
        getSharedPreferences("app", Context.MODE_PRIVATE)
            .getString("language", "en") ?: "en"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatbot_tenant)

        findViewById<MaterialToolbar>(R.id.topAppBar)?.setNavigationOnClickListener { finish() }

        rv = findViewById(R.id.rvChat)
        input = findViewById(R.id.inputChat)
        send = findViewById(R.id.btnSend)
        quickGroup = findViewById(R.id.quickGroup)

        adapter = ChatAdapter(thread)
        rv.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        rv.adapter = adapter

        seedGreeting()
        addQuickChips(
            listOf(
                textFor("rent balance"),
                textFor("my lease"),
                textFor("track maintenance"),
                textFor("report issue"),
                textFor("pay proof upload"),
                textFor("speak to manager")
            )
        )

        send.setOnClickListener {
            val t = input.text?.toString()?.trim().orEmpty()
            if (t.isEmpty()) return@setOnClickListener
            appendMe(t)
            input.text = null
            handleMessage(t)
        }
    }

    // ---------- Quick chips ----------
    private fun addQuickChips(labels: List<String>) {
        quickGroup.removeAllViews()
        val pad = (10 * resources.displayMetrics.density).toInt()
        labels.forEach { label ->
            val tv = TextView(this).apply {
                text = label
                setPadding(pad, pad / 2, pad, pad / 2)
                background = runCatching {
                    resources.getDrawable(R.drawable.bg_quick_chip, theme)
                }.getOrElse {
                    resources.getDrawable(android.R.drawable.btn_default_small, theme)
                }
                setOnClickListener {
                    appendMe(label)
                    handleMessage(label)
                }
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(pad, pad / 2, pad, pad / 2) }
            quickGroup.addView(tv, lp)
        }
    }

    // ---------- Localized seeds ----------
    private fun seedGreeting() {
        val greet = when (lang) {
            "zu" -> "Sawubona! Zama: “rent balance”, “my lease”, “track maintenance”…"
            "st" -> "Lumela! Leka: “rent balance”, “my lease”, “track maintenance”…"
            else -> "Hi! Try: “rent balance”, “my lease”, “track maintenance”…"
        }
        appendBot(greet)
    }

    private fun textFor(english: String) = when (lang) {
        // You can translate these later; keeping English to avoid missing keys.
        "zu", "st" -> english
        else -> english
    }

    // ---------- Chat flow ----------
    private fun handleMessage(text: String) {
        val msg = text.lowercase()

        when {
            msg.contains("rent") && msg.contains("balance") -> { fetchRentBalance(); return }
            msg.contains("my lease") -> { fetchLeaseSummary(); return }
            msg.contains("track") && msg.contains("maintenance") -> { fetchMaintenanceSummary(); return }
            msg.contains("report") && msg.contains("issue") -> {
                appendBot(shortOk())
                startActivity(Intent(this, ReportMaintenanceActivity::class.java))
                return
            }
            msg.contains("pay") && msg.contains("proof") -> {
                appendBot(shortOk())
                startActivity(Intent(this, UploadProofActivity::class.java))
                return
            }
            msg.contains("speak") && msg.contains("manager") -> {
                escalateAutoManager()
                return
            }
        }

        // Fallback to server chatbot if you have one
        sendToServerChatbot(text)
    }

    // ========== AUTO-ESCALATION ==========

    /**
     * Resolve the current tenant's manager via:
     *   1) GET /api/Lease -> find lease with tenantId == meId
     *   2) GET /api/Property -> find that propertyId and read manager.userId
     * Then prompt, POST /api/Message with the payload shape that returned 201 in your logs.
     */
    private fun escalateAutoManager() {
        lifecycleScope.launch {
            try {
                // Step 1: fetch leases
                val leaseRes = withContext(Dispatchers.IO) {
                    api.getAllLeases("Bearer $jwt")
                }
                val leaseList = leaseRes.body().orEmpty()
                val myLease = leaseList.firstOrNull { it.tenantId == meId }
                if (myLease == null) {
                    Log.e("Chatbot", "No lease for tenantId=$meId")
                    appendBot(
                        when (lang) {
                            "zu" -> "Angikwazanga ukuthola i-lease yakho okwamanje."
                            "st" -> "Ha ke fumane konteraka ea hau hona joale."
                            else -> "I couldn’t find your lease right now."
                        }
                    )
                    return@launch
                }
                val propertyId = myLease.propertyId
                Log.d("Chatbot", "Lease found. propertyId=$propertyId")

                // Step 2: fetch properties
                val propRes = withContext(Dispatchers.IO) {
                    api.getAllProperties("Bearer $jwt")
                }
                val property = propRes.body().orEmpty().firstOrNull { it.propertyId == propertyId }
                if (property == null) {
                    Log.e("Chatbot", "Property $propertyId not found in /api/Property")
                    appendBot(errorTxt())
                    return@launch
                }

                val mgr = property.manager
                val managerId = mgr?.userId ?: 0
                if (managerId <= 0) {
                    Log.e("Chatbot", "Property has no manager attached")
                    appendBot(
                        when (lang) {
                            "zu" -> "Ayikho imininingwane yomphathi yaleli propati."
                            "st" -> "Ha ho lintlha tsa mookameli bakeng sa thepa ena."
                            else -> "No manager is assigned to your property."
                        }
                    )
                    return@launch
                }

                // Step 3: ask the tenant what to send
                val et = EditText(this@ChatbotTenantActivity).apply {
                    hint = when (lang) {
                        "zu" -> "Chaza ukuthi yini inkinga…"
                        "st" -> "Hlalosa bothata…"
                        else -> "Describe your issue…"
                    }
                }

                android.app.AlertDialog.Builder(this@ChatbotTenantActivity)
                    .setTitle(
                        when (lang) {
                            "zu" -> "Sitshelani umphathi?"
                            "st" -> "Re bolelle eng ho mookameli?"
                            else -> "What should I tell a property manager?"
                        }
                    )
                    .setView(et)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        val text = et.text?.toString()?.trim().ifNullOrBlank {
                            when (lang) {
                                "zu" -> "Ngicela usizo ngomcimbi wami."
                                "st" -> "Ke kopa thuso ka taba ena."
                                else -> "I need assistance with this issue."
                            }
                        }
                        sendEscalationMessage(managerId, text)
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()

            } catch (t: Throwable) {
                Log.e("Chatbot", "escalateAutoManager error", t)
                appendBot(errorTxt())
            }
        }
    }

    /**
     * Send POST /api/Message with fields matching the 201 success in your logs.
     * Also append a tenant-facing confirmation.
     */
    private fun sendEscalationMessage(managerId: Int, text: String) {
        lifecycleScope.launch {
            try {
                val dto = MessageDto(
                    messageId = 0,
                    senderId = meId,
                    receiverId = managerId,
                    messageText = text,
                    timestamp = null,          // server will set
                    isChatbot = true,          // mark as chatbot escalation
                    isBroadcast = false,
                    isEscalated = false
                )

                val res: Response<MessageDto> = withContext(Dispatchers.IO) {
                    api.sendMessage("Bearer $jwt", dto).execute()
                }

                if (res.isSuccessful) {
                    Log.d("Chatbot", "Escalation sent -> managerId=$managerId code=${res.code()}")
                    appendBot(
                        when (lang) {
                            "zu" -> "Ngithumele umlayezo ku **umphathi wepropati**. Ungaqhubeka nokuxoxa nabo emiyalezweni yakho."
                            "st" -> "Ke rometse molaetsa ho **mookameli wa thepa**. O ka tsoela pele ho qoqa le yena Melaetseng."
                            else -> "I’ve messaged **a property manager** for you. You can continue the chat in your Messages."
                        }
                    )
                } else {
                    Log.e("Chatbot", "Message POST failed: ${res.code()}")
                    appendBot(errorTxt())
                }
            } catch (t: Throwable) {
                Log.e("Chatbot", "sendEscalationMessage error", t)
                appendBot(errorTxt())
            }
        }
    }



    // ========== Server chatbot fallback (no changes needed) ==========
    private fun sendToServerChatbot(text: String) {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            appendBot(
                when (lang) {
                    "zu" -> "Awukho ku-inthanethi — ngizoyifaka ohlwini lokuthumela kamuva."
                    "st" -> "Ha o inthaneteng — ke tla e romela hamorao."
                    else -> "You’re offline — I’ll queue that to send later."
                }
            )
            failStreak = 0
            return
        }

        lifecycleScope.launch {
            try {
                val body = ApiService.ChatbotMessageCreate(
                    messageText = text,
                    isChatbot = true,
                    language = lang
                )

                val anyResp: Any = withContext(Dispatchers.IO) {
                    api.sendChatbotMessage("Bearer $jwt", body) as Any
                }

                when (anyResp) {
                    is retrofit2.Response<*> -> {
                        if (anyResp.isSuccessful) {
                            failStreak = 0
                            val md = anyResp.body() as? MessageDto
                            val replyText = md?.messageText ?: inboxHint()
                            appendBot(replyText)
                        } else {
                            bumpFailAndMaybeEscalate(text)
                        }
                    }
                    is retrofit2.Call<*> -> {
                        val executed = withContext(Dispatchers.IO) {
                            (anyResp as retrofit2.Call<MessageDto>).execute()
                        }
                        if (executed.isSuccessful) {
                            failStreak = 0
                            val md = executed.body()
                            appendBot(md?.messageText ?: inboxHint())
                        } else {
                            bumpFailAndMaybeEscalate(text)
                        }
                    }
                    is MessageDto -> {
                        failStreak = 0
                        appendBot(anyResp.messageText?.takeIf { it.isNotBlank() } ?: inboxHint())
                    }
                    else -> bumpFailAndMaybeEscalate(text)
                }
            } catch (_: Exception) {
                bumpFailAndMaybeEscalate(text)
            }
        }
    }

    // ---------- Data pulls / summaries (unchanged from your working bits) ----------
    private fun fetchRentBalance() {
        lifecycleScope.launch {
            try {
                val paymentsRes = withContext(Dispatchers.IO) {
                    api.getTenantPayments("Bearer $jwt", meId)
                }
                val leaseRes = withContext(Dispatchers.IO) {
                    api.getAllLeases("Bearer $jwt")
                }

                val rent = leaseRes.body()
                    ?.firstOrNull { it.tenantId == meId }
                    ?.property?.rentAmount ?: 0.0

                val last3 = paymentsRes.body().orEmpty()
                    .sortedByDescending { it.date ?: "" }
                    .take(3)

                val lines = buildList {
                    add(h2(when (lang) {
                        "zu" -> "Isifinyezo seNkokhelo"
                        "st" -> "Kakaretšo ea Tefo"
                        else -> "Payment Summary"
                    }))
                    add("Rent (monthly): R${fmt(rent)}")
                    if (last3.isEmpty()) {
                        add(
                            when (lang) {
                                "zu" -> "Awekho amarekhodi okukhokha."
                                "st" -> "Ha ho lirekoto tsa tefo."
                                else -> "No payment records yet."
                            }
                        )
                    } else {
                        last3.forEach {
                            val d = (it.date ?: "").take(10)
                            val s = it.status ?: "Pending"
                            val amt = it.amount ?: 0.0
                            add("• $d — R${fmt(amt)} — $s")
                        }
                    }
                }
                appendBot(lines.joinToString("\n"))
            } catch (_: Exception) {
                appendBot(errorTxt())
            }
        }
    }

    private fun fetchLeaseSummary() {
        lifecycleScope.launch {
            try {
                val leaseRes = withContext(Dispatchers.IO) {
                    api.getAllLeases("Bearer $jwt")
                }
                val lease = leaseRes.body().orEmpty().firstOrNull { it.tenantId == meId }
                if (lease == null) {
                    appendBot(
                        when (lang) {
                            "zu" -> "Ayikho i-lease esebenzayo etholakele."
                            "st" -> "Ha ho konteraka e sebetsang e fumanoeng."
                            else -> "No active lease found."
                        }
                    )
                    return@launch
                }
                val p = lease.property
                val lines = listOf(
                    h2(when (lang) { "zu" -> "Imininingwane ye-Lease"; "st" -> "Lintlha tsa Konteraka"; else -> "Lease Summary" }),
                    "Property: ${p?.title ?: "-"}",
                    "Address: ${p?.address ?: "-"}",
                    "Rent: R${fmt(p?.rentAmount ?: 0.0)}",
                    "Start: ${lease.startDate.take(10)}",
                    "End: ${lease.endDate.take(10)}"
                )
                appendBot(lines.joinToString("\n"))
            } catch (_: Exception) {
                appendBot(errorTxt())
            }
        }
    }

    private fun fetchMaintenanceSummary() {
        lifecycleScope.launch {
            try {
                val res = withContext(Dispatchers.IO) { api.getMyRequests("Bearer $jwt") }
                val items = res.body().orEmpty().sortedByDescending { it.createdAt ?: "" }
                if (items.isEmpty()) {
                    appendBot(
                        when (lang) {
                            "zu" -> "Awunazo izicelo zokulungisa okwamanje."
                            "st" -> "Ha ho likopo tsa tlhokomelo hajoale."
                            else -> "You have no maintenance requests yet."
                        }
                    )
                    return@launch
                }
                val open = items.count {
                    (it.status ?: "").contains("Pending", true) || (it.status ?: "").contains("In Progress", true)
                }
                val lines = buildList {
                    add(h2(when (lang) { "zu" -> "Isifinyezo Sokulungisa"; "st" -> "Kakaretšo ea Tlhokomelo"; else -> "Maintenance Summary" }))
                    add("Open: $open • Total: ${items.size}")
                    add("--- Latest ---")
                    items.take(5).forEach {
                        val d = (it.createdAt ?: "").take(10)
                        add("• #${it.maintenanceId} — ${it.status ?: "-"} — $d")
                    }
                }
                appendBot(lines.joinToString("\n"))
            } catch (_: Exception) {
                appendBot(errorTxt())
            }
        }
    }

    // ---------- failure handling ----------
    private fun inboxHint() = when (lang) {
        "zu" -> "Ngithumele impendulo yakho emibikweni — sicela ubheke imiyalezo."
        "st" -> "Karabo e rometsoe ka Melaetšong — sheba moo."
        else -> "I’ve sent my reply to your Messages — check your inbox."
    }

    private fun bumpFailAndMaybeEscalate(lastText: String) {
        failStreak = (failStreak + 1).coerceAtMost(3)
        if (failStreak >= 3) {
            failStreak = 0
            escalateAutoManager()
        } else {
            appendBot(
                when (lang) {
                    "zu" -> "Angikwazanga ukusiza ngalokho. Zama ngamanye amazwi."
                    "st" -> "Ha ke a khonahala hona joale. Leka ho e hlalosa ka tsela e ’ngoe."
                    else -> "I couldn’t answer that. Try rephrasing."
                }
            )
        }
    }

    // ---------- UI helpers ----------
    private fun appendMe(text: String) {
        val md = MessageDto(
            messageId = 0,
            senderId = meId,
            receiverId = 0,
            messageText = text,
            timestamp = nowIso(),
            isChatbot = false
        )
        adapter.append(md, rv)
    }

    private fun appendBot(text: String) {
        val md = MessageDto(
            messageId = 0,
            senderId = 0,
            receiverId = meId,
            messageText = text,
            timestamp = nowIso(),
            isChatbot = true
        )
        adapter.append(md, rv)
    }

    private fun nowIso(): String {
        val now = System.currentTimeMillis()
        return DateFormat.format("yyyy-MM-dd HH:mm:ss", now).toString()
    }

    private fun fmt(v: Double) = String.format("%,.2f", v)

    private fun h2(t: String) = "— $t —"

    private fun shortOk() = when (lang) {
        "zu" -> "Kulungile."
        "st" -> "Ho lokile."
        else -> "Ok."
    }

    private fun errorTxt() = when (lang) {
        "zu" -> "Kwenzeke iphutha. Sicela uzame futhi."
        "st" -> "Ho bile le phoso. Leka hape."
        else -> "Something went wrong. Try again."
    }

    private inline fun String?.ifNullOrBlank(block: () -> String): String {
        val s = this
        return if (s == null || s.isBlank()) block() else s
    }

}
