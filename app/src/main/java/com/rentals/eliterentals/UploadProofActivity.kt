package com.rentals.eliterentals

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.*

class UploadProofActivity : AppCompatActivity() {

    private val PICK_FILE_REQUEST = 1
    private var fileUri: Uri? = null

    private lateinit var txtFileName: TextView
    private lateinit var txtStatus: TextView
    private lateinit var btnChooseFile: Button
    private lateinit var btnUpload: Button
    private lateinit var etAmount: EditText
    private lateinit var etDate: EditText

    private lateinit var authToken: String
    private var tenantId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_proof)

        txtFileName = findViewById(R.id.txtFileName)
        txtStatus = findViewById(R.id.txtStatus)
        btnChooseFile = findViewById(R.id.btnChooseFile)
        btnUpload = findViewById(R.id.btnUpload)
        etAmount = findViewById(R.id.etAmount)
        etDate = findViewById(R.id.etDate)

        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        authToken = prefs.getString("jwt", "") ?: ""
        tenantId = prefs.getInt("userId", -1)

        if (tenantId == -1) {
            txtStatus.text = "Tenant ID not found. Please log in again."
            btnUpload.isEnabled = false
            return
        }

        btnChooseFile.setOnClickListener { openFileChooser() }
        btnUpload.setOnClickListener { submitPayment() }

        etDate.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                etDate.setText(String.format("%04d-%02d-%02d", y, m + 1, d))
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }

        // Bottom nav
        findViewById<LinearLayout>(R.id.navDashboard).setOnClickListener {
            startActivity(Intent(this, TenantDashboardActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.navMaintenance).setOnClickListener {
            startActivity(Intent(this, ReportMaintenanceActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.navPayments).setOnClickListener { }
        findViewById<LinearLayout>(R.id.navSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
        }
    }

    private fun openFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        startActivityForResult(intent, PICK_FILE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
            fileUri = data?.data
            txtFileName.text = fileUri?.lastPathSegment ?: "Unknown file"
        }
    }

    private fun submitPayment() {
        val amount = etAmount.text.toString().trim()
        val date = etDate.text.toString().trim()

        if (fileUri == null) {
            txtStatus.text = "Please choose a file first."
            return
        }
        if (amount.isEmpty() || date.isEmpty()) {
            txtStatus.text = "Please enter both amount and date."
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)
            val online = isOnline()

            val payment = OfflinePayment(
                tenantId = tenantId,
                amount = amount,
                dateIso = "${date}T00:00:00Z",
                fileName = txtFileName.text.toString(),
                fileUri = fileUri.toString(),
                syncStatus = if (online) "synced" else "pending"
            )

            db.offlineDao().insertPayment(payment)

            if (online) {
                try {
                    val inputStream: InputStream? = contentResolver.openInputStream(fileUri!!)
                    val tempFile = File.createTempFile("proof_", txtFileName.text.toString())
                    val out = FileOutputStream(tempFile)
                    val buffer = ByteArray(1024)
                    var len: Int
                    while (inputStream!!.read(buffer).also { len = it } != -1) {
                        out.write(buffer, 0, len)
                    }
                    out.close()
                    inputStream.close()

                    val fileBody = tempFile.asRequestBody("application/octet-stream".toMediaType())
                    val requestBody = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("TenantId", tenantId.toString())
                        .addFormDataPart("Amount", amount)
                        .addFormDataPart("Date", payment.dateIso)
                        .addFormDataPart("proof", txtFileName.text.toString(), fileBody)
                        .build()

                    val client = okhttp3.OkHttpClient()
                    val request = okhttp3.Request.Builder()
                        .url("https://eliterentalsapi-czckh7fadmgbgtgf.southafricanorth-01.azurewebsites.net/api/payment")
                        .addHeader("Authorization", "Bearer $authToken")
                        .post(requestBody)
                        .build()

                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) {
                        payment.syncStatus = "pending"
                        db.offlineDao().updatePayment(payment)
                    }

                } catch (e: Exception) {
                    payment.syncStatus = "pending"
                    db.offlineDao().updatePayment(payment)
                }
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@UploadProofActivity,
                    if (online) "Payment submitted successfully" else "Saved locally — will sync when online",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    private fun isOnline(): Boolean {
        val cm = getSystemService(ConnectivityManager::class.java)
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
