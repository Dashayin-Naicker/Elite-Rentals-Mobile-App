package com.rentals.eliterentals

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.rentals.eliterentals.utils.FileUtils
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File

class ReportMaintenanceActivity : AppCompatActivity() {

    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerUrgency: Spinner
    private lateinit var etDescription: EditText
    private lateinit var btnSubmit: Button
    private lateinit var ivUpload: ImageView
    private lateinit var uploadLayout: LinearLayout
    private var selectedUri: Uri? = null

    companion object {
        private const val REQUEST_IMAGE_PICK = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maintenance_request)

        spinnerCategory = findViewById(R.id.spinnerCategory)
        spinnerUrgency = findViewById(R.id.spinnerUrgency)
        etDescription = findViewById(R.id.etDescription)
        btnSubmit = findViewById(R.id.btnSubmit)
        uploadLayout = findViewById(R.id.uploadContainer)
        ivUpload = findViewById(R.id.ivBack)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        // Populate spinners
        spinnerCategory.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Plumbing", "Electrical", "Appliances", "Structural", "Other")
        )

        spinnerUrgency.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Low", "Medium", "High", "Critical")
        )

        // Upload photo click
        uploadLayout.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            startActivityForResult(intent, REQUEST_IMAGE_PICK)
        }

        // Submit maintenance request
        btnSubmit.setOnClickListener {
            submitMaintenance()
        }
    }

    private fun submitMaintenance() {
        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        val jwt = prefs.getString("jwt", null)
        val tenantId = prefs.getInt("userId", -1)
        val propertyId = prefs.getInt("propertyId", -1) // You can set this earlier when tenant logs in

        val category = spinnerCategory.selectedItem.toString()
        val urgency = spinnerUrgency.selectedItem.toString()
        val description = etDescription.text.toString().trim()

        if (jwt == null || tenantId == -1) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            return
        }

        if (propertyId == -1) {
            Toast.makeText(this, "No property linked to tenant", Toast.LENGTH_SHORT).show()
            return
        }

        if (description.isEmpty()) {
            Toast.makeText(this, "Please enter a description", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val tenantPart = RequestBody.create("text/plain".toMediaTypeOrNull(), tenantId.toString())
                val propertyPart = RequestBody.create("text/plain".toMediaTypeOrNull(), propertyId.toString())
                val descriptionPart = RequestBody.create("text/plain".toMediaTypeOrNull(), description)
                val categoryPart = RequestBody.create("text/plain".toMediaTypeOrNull(), category)
                val urgencyPart = RequestBody.create("text/plain".toMediaTypeOrNull(), urgency)

                val proofPart = selectedUri?.let {
                    val filePath = FileUtils.getPath(this@ReportMaintenanceActivity, it)
                    val file = File(filePath!!)
                    val requestFile = RequestBody.create(contentResolver.getType(it)?.toMediaTypeOrNull(), file)
                    MultipartBody.Part.createFormData("proof", file.name, requestFile)
                }

                val response = RetrofitClient.instance.createMaintenance(
                    "Bearer $jwt",
                    tenantPart,
                    propertyPart,
                    descriptionPart,
                    categoryPart,
                    urgencyPart,
                    proofPart
                )


                if (response.isSuccessful) {
                    Toast.makeText(this@ReportMaintenanceActivity, "Maintenance request submitted successfully", Toast.LENGTH_LONG).show()

                    // Navigate back to tenant dashboard
                    val intent = Intent(this@ReportMaintenanceActivity, TenantDashboardActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                }
                else {
                    Toast.makeText(this@ReportMaintenanceActivity, "Failed to submit request", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@ReportMaintenanceActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK) {
            selectedUri = data?.data
            if (selectedUri != null) {
                Toast.makeText(this, "Photo selected", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
