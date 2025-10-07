package com.rentals.eliterentals

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
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

    private val baseUrl =
        "https://eliterentalsapi-czckh7fadmgbgtgf.southafricanorth-01.azurewebsites.net/api/payment"

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

        // Retrieve saved login info
        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        authToken = prefs.getString("jwt", "") ?: ""
        tenantId = prefs.getInt("userId", -1)

        if (tenantId == -1) {
            txtStatus.text = "Error: Tenant ID not found. Please log in again."
            btnUpload.isEnabled = false
            return
        }

        btnChooseFile.setOnClickListener { openFileChooser() }
        btnUpload.setOnClickListener { uploadPayment() }

        // Open date picker
        etDate.setOnClickListener {
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(this, { _, y, m, d ->
                val selectedDate = String.format("%04d-%02d-%02d", y, m + 1, d)
                etDate.setText(selectedDate)
            }, year, month, day)

            datePicker.show()
        }
        // Bottom navigation functionality
        findViewById<LinearLayout>(R.id.navDashboard).setOnClickListener {
            startActivity(Intent(this, TenantDashboardActivity::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.navMaintenance).setOnClickListener {
            startActivity(Intent(this, ReportMaintenanceActivity::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.navPayments).setOnClickListener {
            // Already on this screen, no action needed
        }

        findViewById<LinearLayout>(R.id.navSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
        }

    }

    private fun openFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*" // allow any file (pdf, jpg, png)
        startActivityForResult(intent, PICK_FILE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
            fileUri = data?.data
            txtFileName.text = fileUri?.let { getFileName(it) } ?: "Unknown file"
        }
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
            cursor.use {
                if (it != null && it.moveToFirst()) {
                    result = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) result = result?.substring(cut + 1)
        }
        return result ?: "file"
    }

    private fun uploadPayment() {
        val amountText = etAmount.text.toString().trim()
        val dateText = etDate.text.toString().trim()

        if (fileUri == null) {
            txtStatus.text = "Please choose a file first."
            return
        }
        if (amountText.isEmpty() || dateText.isEmpty()) {
            txtStatus.text = "Please enter both amount and date."
            return
        }

        try {
            val inputStream: InputStream? = contentResolver.openInputStream(fileUri!!)
            val tempFile = File.createTempFile("proof_", getFileName(fileUri!!))
            val out = FileOutputStream(tempFile)
            val buffer = ByteArray(1024)
            var len: Int
            while (inputStream!!.read(buffer).also { len = it } != -1) {
                out.write(buffer, 0, len)
            }
            out.close()
            inputStream.close()

            val client = OkHttpClient()
            val fileBody = tempFile.asRequestBody("application/octet-stream".toMediaType())
            val isoDate = "${dateText}T00:00:00Z"

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("TenantId", tenantId.toString())
                .addFormDataPart("Amount", amountText)
                .addFormDataPart("Date", isoDate)
                .addFormDataPart("proof", getFileName(fileUri!!), fileBody)
                .build()

            val request = Request.Builder()
                .url(baseUrl)
                .addHeader("Authorization", "Bearer $authToken")
                .post(requestBody)
                .build()

            Thread {
                try {
                    val response = client.newCall(request).execute()
                    runOnUiThread {
                        if (response.isSuccessful) {
                            Toast.makeText(this@UploadProofActivity, "Payment uploaded ✅", Toast.LENGTH_SHORT).show()

                            // Redirect to TenantDashboardActivity
                            val intent = Intent(this@UploadProofActivity, TenantDashboardActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            txtStatus.text = "Upload failed: ${response.code} ${response.message} \n${response.body?.string()}"
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        txtStatus.text = "Error: ${e.message}"
                    }
                }
            }.start()

        } catch (e: Exception) {
            txtStatus.text = "Error: ${e.message}"
        }
    }
}
