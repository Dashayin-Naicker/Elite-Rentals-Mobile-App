package com.rentals.eliterentals

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.File

class PmPaymentDetailActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_PAYMENT_ID = "extra_payment_id"
        private const val EXTRA_TENANT_ID = "extra_tenant_id"
        private const val EXTRA_AMOUNT = "extra_amount"
        private const val EXTRA_STATUS = "extra_status"
        private const val EXTRA_DATE = "extra_date"
        private const val EXTRA_METHOD = "extra_method"
        private const val EXTRA_PROOF_TYPE = "extra_proof_type"

        fun createIntent(
            context: Context,
            payment: PaymentDto
        ): Intent {
            return Intent(context, PmPaymentDetailActivity::class.java).apply {
                putExtra(EXTRA_PAYMENT_ID, payment.paymentId)
                putExtra(EXTRA_TENANT_ID, payment.tenantId)
                putExtra(EXTRA_AMOUNT, payment.amount)
                putExtra(EXTRA_STATUS, payment.status)
                putExtra(EXTRA_DATE, payment.date)
                putExtra(EXTRA_METHOD, payment.method)
                putExtra(EXTRA_PROOF_TYPE, payment.proofType)
            }
        }
    }

    private var paymentId: Int = 0
    private var originalStatus: String = "Pending"
    private var proofType: String? = null

    private lateinit var txtTenant: TextView
    private lateinit var txtAmount: TextView
    private lateinit var txtDate: TextView
    private lateinit var txtCurrentStatus: TextView
    private lateinit var txtMethod: TextView
    private lateinit var txtProofType: TextView
    private lateinit var btnViewProof: Button
    private lateinit var btnUpdatePaid: Button
    private lateinit var btnUpdatePending: Button
    private lateinit var progress: ProgressBar
    private lateinit var txtError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pm_payment_detail)
        findViewById<ImageView>(R.id.btnBackDetail).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        supportActionBar?.title = "Payment Details"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        txtTenant = findViewById(R.id.txtTenant)
        txtAmount = findViewById(R.id.txtAmount)
        txtDate = findViewById(R.id.txtDate)
        txtCurrentStatus = findViewById(R.id.txtCurrentStatus)
        txtMethod = findViewById(R.id.txtMethod)
        txtProofType = findViewById(R.id.txtProofType)
        btnViewProof = findViewById(R.id.btnViewProof)
        btnUpdatePaid = findViewById(R.id.btnUpdatePaid)
        btnUpdatePending = findViewById(R.id.btnUpdatePending)
        progress = findViewById(R.id.progressBar)
        txtError = findViewById(R.id.txtError)

        paymentId = intent.getIntExtra(EXTRA_PAYMENT_ID, 0)
        val tenantId = intent.getIntExtra(EXTRA_TENANT_ID, 0)
        val amount = intent.getDoubleExtra(EXTRA_AMOUNT, 0.0)
        val date = intent.getStringExtra(EXTRA_DATE) ?: ""
        originalStatus = intent.getStringExtra(EXTRA_STATUS) ?: "Pending"
        proofType = intent.getStringExtra(EXTRA_PROOF_TYPE)

        // Display fields EXACTLY as API provides
        txtTenant.text = "Tenant ID: $tenantId"
        txtAmount.text = "Amount: R %.2f".format(amount)
        txtDate.text = "Date: $date"
        txtCurrentStatus.text = "Status: $originalStatus"
        txtProofType.text = "Proof Type: ${proofType ?: "None"}"
        txtMethod.visibility = View.GONE

        btnViewProof.setOnClickListener { viewProof() }
        btnUpdatePaid.setOnClickListener { updateStatus("Paid") }
        btnUpdatePending.setOnClickListener { updateStatus("Rejected") }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun viewProof() {
        val token = SharedPrefs.getToken(this)
        if (token.isBlank()) {
            Toast.makeText(this, "Missing auth token. Please log in again.", Toast.LENGTH_LONG).show()
            return
        }

        txtError.visibility = View.GONE
        progress.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.downloadProof(
                        bearer = "Bearer $token",
                        paymentId = paymentId
                    )
                }

                when {
                    response.isSuccessful -> {
                        val body = response.body()
                        if (body != null) {
                            openProofFile(body)
                        } else {
                            showError("Empty proof file.")
                        }
                    }
                    response.code() == 404 -> {
                        showError("No proof of payment uploaded for this payment.")
                    }
                    else -> {
                        showError("Failed to download proof (${response.code()}).")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showError("Unexpected error downloading proof.")
            } finally {
                progress.visibility = View.GONE
            }
        }
    }

    private fun detectFileExtension(bytes: ByteArray): String {
        return when {
            bytes.size > 4 && bytes[0] == 0x25.toByte() && bytes[1] == 0x50.toByte() &&
                    bytes[2] == 0x44.toByte() && bytes[3] == 0x46.toByte() -> "pdf" // %PDF
            bytes.size > 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() &&
                    bytes[2] == 0xFF.toByte() -> "jpg" // JPEG
            bytes.size > 4 && bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
                    bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte() -> "png" // PNG
            else -> "bin"
        }
    }

    private fun openProofFile(body: ResponseBody) {
        val bytes = body.bytes()
        val ext = detectFileExtension(bytes)

        val file = File(cacheDir, "payment_proof_${paymentId}.$ext")
        file.writeBytes(bytes)

        val uri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.provider",
            file
        )

        when (ext) {
            "jpg", "png" -> {
                val intent = Intent(this, ViewPhotoActivity::class.java)
                intent.putExtra("imageUrl", uri.toString())
                startActivity(intent)
            }
            "pdf" -> {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try {
                    startActivity(Intent.createChooser(intent, "Open Payment Proof"))
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(this, "No app found to open PDF", Toast.LENGTH_LONG).show()
                }
            }
            else -> Toast.makeText(this, "Unsupported proof type", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateStatus(newStatus: String) {
        val token = SharedPrefs.getToken(this)
        if (token.isBlank()) {
            Toast.makeText(this, "Missing auth token. Please log in again.", Toast.LENGTH_LONG).show()
            return
        }

        txtError.visibility = View.GONE
        progress.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.updatePaymentStatus(
                        bearer = "Bearer $token",
                        paymentId = paymentId,
                        dto = PaymentStatusDto(status = newStatus)
                    )
                }

                if (response.isSuccessful) {
                    Toast.makeText(this@PmPaymentDetailActivity, "Status updated.", Toast.LENGTH_SHORT).show()
                    txtCurrentStatus.text = "Status: $newStatus"
                } else {
                    showError("Failed to update status (${response.code()}).")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showError("Unexpected error updating status.")
            } finally {
                progress.visibility = View.GONE
            }
        }
    }

    private fun showError(message: String) {
        txtError.text = message
        txtError.visibility = View.VISIBLE
    }
}
