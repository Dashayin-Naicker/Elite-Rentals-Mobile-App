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
        val method = intent.getStringExtra(EXTRA_METHOD) ?: "Unknown"
        proofType = intent.getStringExtra(EXTRA_PROOF_TYPE)

        txtTenant.text = "Tenant #$tenantId"
        txtAmount.text = "R %.2f".format(amount)
        txtDate.text = date
        txtCurrentStatus.text = originalStatus
        txtMethod.text = "Method: $method"
        txtProofType.text = "Proof type: ${proofType ?: "Unknown"}"

        btnViewProof.setOnClickListener {
            viewProof()
        }

        btnUpdatePaid.setOnClickListener {
            updateStatus("Paid")
        }

        btnUpdatePending.setOnClickListener {
            updateStatus("Revoked")
        }
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
                        // Match website behaviour: no proof uploaded
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


    private fun openProofFile(body: ResponseBody) {
        val contentType = body.contentType()?.toString() ?: ""
        val ext = when {
            contentType.contains("pdf", ignoreCase = true) -> "pdf"
            contentType.contains("png", ignoreCase = true) -> "png"
            contentType.contains("jpeg", ignoreCase = true) ||
                    contentType.contains("jpg", ignoreCase = true) -> "jpg"
            else -> {
                // try to infer from proofType
                val pt = proofType?.lowercase()
                when {
                    pt?.contains("pdf") == true || pt?.contains("doc") == true -> "pdf"
                    pt?.contains("image") == true -> "jpg"
                    else -> "bin"
                }
            }
        }

        val file = File(cacheDir, "payment_proof_${paymentId}.$ext")
        file.outputStream().use { output ->
            body.byteStream().use { input ->
                input.copyTo(output)
            }
        }

        val uri: Uri = FileProvider.getUriForFile(
            this,
            "$packageName.provider",
            file
        )


        if (ext == "png" || ext == "jpg") {
            // Use existing photo viewer
            val intent = Intent(this, ViewPhotoActivity::class.java).apply {
                putExtra("imageUrl", uri.toString())
            }
            startActivity(intent)
        } else if (ext == "pdf") {
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try {
                startActivity(Intent.createChooser(viewIntent, "Open payment proof"))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "No app found to open PDF", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Unsupported proof type", Toast.LENGTH_LONG).show()
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
                    txtCurrentStatus.text = newStatus
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
