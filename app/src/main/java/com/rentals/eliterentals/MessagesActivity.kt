package com.rentals.eliterentals

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MessagesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MessageAdapter
    private val messages = mutableListOf<MessageDto>()

    private companion object { const val MENU_ASSISTANT_ID = 1001 }
    private var chatbotFailStreak = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)

        recyclerView = findViewById(R.id.recyclerViewMessages)
        val btnViewAnnouncements = findViewById<Button>(R.id.btnViewAnnouncements)

        adapter = MessageAdapter(messages) { message ->
            val intent = Intent(this, ReplyActivity::class.java)
            intent.putExtra("receiverId", message.senderId)
            startActivity(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnViewAnnouncements.setOnClickListener {
            startActivity(Intent(this, AnnouncementsActivity::class.java))
        }

        fetchInbox()

        // 🔹 If launched with Ask Assistant from the dashboard tile, open the dialog immediately.
        if (intent.getBooleanExtra("askAssistant", false)) {
            openAskAssistantDialog()
        }
    }

    // Also handle the case where the activity is already running and receives a new intent.
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.getBooleanExtra("askAssistant", false) == true) {
            openAskAssistantDialog()
        }
    }

    // ---------- MENU: Ask Assistant ----------
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(Menu.NONE, MENU_ASSISTANT_ID, Menu.NONE, "Ask Assistant")
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            MENU_ASSISTANT_ID -> { openAskAssistantDialog(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openAskAssistantDialog() {
        val input = EditText(this).apply { hint = "Type your question…"; setSingleLine() }
        val presets = arrayOf("rent balance","pay proof upload","my lease","report issue","track maintenance","speak to manager")

        AlertDialog.Builder(this)
            .setTitle("Ask Assistant")
            .setView(input)
            .setNeutralButton("Suggestions") { _, _ ->
                AlertDialog.Builder(this)
                    .setTitle("Quick intents")
                    .setItems(presets) { _: DialogInterface, which: Int -> input.setText(presets[which]) }
                    .setPositiveButton("OK", null)
                    .show()
            }
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Send") { _, _ ->
                val text = input.text?.toString()?.trim().orEmpty()
                if (text.isNotEmpty()) sendChatbot(text)
            }
            .show()
    }

    // ---------- CHATBOT send ----------
    private fun appLang(): String {
        val p = getSharedPreferences("app", MODE_PRIVATE)
        return p.getString("language", "en") ?: "en"   // keys: "en" | "zu" | "st"
    }

    private fun sentMsg()   = when (appLang()) { "zu"->"Kuthunyelwe. Phendula izofika lapha."; "st"->"E rometsoe. Karabo e tla fihla mona."; else->"Sent. I’ll reply here when ready." }
    private fun queuedMsg() = when (appLang()) { "zu"->"Awukho ku-inthanethi — ngikufake ohlwini."; "st"->"Ha o inthaneteng — ke e kentse lenaneng."; else->"You’re offline — I queued your question." }
    private fun failMsg()   = when (appLang()) { "zu"->"Angikwazanga ukuthumela manje."; "st"->"Ke sitiloe ho romela hona joale."; else->"I couldn’t send that right now." }
    private fun escalated() = when (appLang()) { "zu"->"Imizamo emi-3 — sengikudlulisele kumphathi."; "st"->"Liteko tse 3 — e fetisitsoe ho mookameli."; else->"After 3 tries, I’ve escalated this to a manager." }

    private fun sendChatbot(text: String) {
        // Quick local intents to help immediately
        if (handleLocalIntent(text)) return

        val token = SharedPrefs.getToken(this).orEmpty()

        // Offline? schedule a one-off SyncWorker job with the chatbot payload
        if (!NetworkUtils.isNetworkAvailable(this)) {
            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val data = Data.Builder()
                .putString("jwt", token)
                .putString("chatbot_text", text)
                .putString("chatbot_lang", appLang())
                .build()
            val req = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setInputData(data)
                .build()
            WorkManager.getInstance(this).enqueue(req)
            Toast.makeText(this, queuedMsg(), Toast.LENGTH_LONG).show()
            return
        }

        // Online: call /api/Message with isChatbot=true
        lifecycleScope.launch {
            try {
                val svc  = RetrofitClient.instance
                val resp = withContext(Dispatchers.IO) {
                    svc.sendChatbotMessage(
                        bearer = "Bearer $token",
                        body = ApiService.ChatbotMessageCreate(
                            messageText = text,
                            isChatbot = true,
                            language = appLang()
                        )
                    )
                }
                if (resp.isSuccessful) {
                    chatbotFailStreak = 0
                    Toast.makeText(this@MessagesActivity, sentMsg(), Toast.LENGTH_SHORT).show()
                    // Optionally refresh inbox to show bot reply
                    fetchInbox()
                } else {
                    bumpFailAndMaybeEscalate(text, token)
                }
            } catch (_: Exception) {
                bumpFailAndMaybeEscalate(text, token)
            }
        }
    }

    private fun handleLocalIntent(msg: String): Boolean {
        val m = msg.lowercase()
        return when {
            m.contains("rent") && m.contains("balance") -> { Toast.makeText(this,"Open Payments to see your balance.",Toast.LENGTH_SHORT).show(); true }
            m.contains("pay") && m.contains("proof")     -> { Toast.makeText(this,"Use ‘Upload Proof’ under Payments.",Toast.LENGTH_SHORT).show(); true }
            m.contains("my lease")                      -> { Toast.makeText(this,"Open Lease to view your details.",Toast.LENGTH_SHORT).show(); true }
            m.contains("report") && m.contains("issue") -> { startActivity(Intent(this, ReportMaintenanceActivity::class.java)); true }
            m.contains("track") && m.contains("maintenance")-> { startActivity(Intent(this, TrackMaintenanceActivity::class.java)); true }
            m.contains("speak") && m.contains("manager")-> { Toast.makeText(this,"We’ll notify your manager. You can also send a message.",Toast.LENGTH_SHORT).show(); true }
            else -> false
        }
    }

    private fun bumpFailAndMaybeEscalate(lastMsg: String, token: String) {
        chatbotFailStreak = (chatbotFailStreak + 1).coerceAtMost(3)
        if (chatbotFailStreak >= 3) {
            lifecycleScope.launch {
                try {
                    val svc = RetrofitClient.instance
                    withContext(Dispatchers.IO) {
                        svc.createReport(
                            bearer = "Bearer $token",
                            body = mapOf(
                                "title" to "Tenant chatbot escalation",
                                "description" to "Couldn’t answer after 3 tries.\nLast: $lastMsg",
                                "type" to "Escalation",
                                "status" to "Open"
                            )
                        )
                    }
                } catch (_: Exception) { /* ignore */ }
                Toast.makeText(this@MessagesActivity, escalated(), Toast.LENGTH_LONG).show()
                chatbotFailStreak = 0
            }
        } else {
            Toast.makeText(this, failMsg(), Toast.LENGTH_LONG).show()
        }
    }

    // ---------- existing inbox flow ----------
    private val userMap = mutableMapOf<Int, UserDto>()

    private fun fetchInbox() {
        val token = SharedPrefs.getToken(this)
        val userId = SharedPrefs.getUserId(this)

        RetrofitClient.instance.getInboxMessages("Bearer $token", userId)
            .enqueue(object : Callback<List<MessageDto>> {
                override fun onResponse(call: Call<List<MessageDto>>, response: Response<List<MessageDto>>) {
                    if (response.isSuccessful) {
                        val allMessages = response.body() ?: emptyList()
                        val latestMessages = allMessages
                            .groupBy { it.senderId }
                            .mapNotNull { (_, msgs) -> msgs.maxByOrNull { it.timestamp ?: "" } }

                        messages.clear()
                        messages.addAll(latestMessages.sortedByDescending { it.timestamp })
                        fetchSenderRoles(token ?: "")
                    }
                }
                override fun onFailure(call: Call<List<MessageDto>>, t: Throwable) {
                    Toast.makeText(this@MessagesActivity, "Network error", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun fetchSenderRoles(token: String) {
        val uniqueSenderIds = messages.map { it.senderId }.distinct()
        uniqueSenderIds.forEach { senderId ->
            RetrofitClient.instance.getUserById("Bearer $token", senderId)
                .enqueue(object : Callback<UserDto> {
                    override fun onResponse(call: Call<UserDto>, response: Response<UserDto>) {
                        if (response.isSuccessful) {
                            response.body()?.let { user ->
                                userMap[user.userId] = user
                                adapter.notifyDataSetChanged()
                            }
                        }
                    }
                    override fun onFailure(call: Call<UserDto>, t: Throwable) { /* ignore */ }
                })
        }
    }
}
