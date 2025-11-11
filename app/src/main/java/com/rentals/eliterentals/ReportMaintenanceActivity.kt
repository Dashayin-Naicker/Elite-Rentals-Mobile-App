package com.rentals.eliterentals

import android.app.Activity
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.rentals.eliterentals.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File

class ReportMaintenanceActivity : BaseActivity() {

    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerUrgency: Spinner
    private lateinit var etDescription: EditText
    private lateinit var btnSubmit: Button
    private lateinit var ivUpload: ImageView
    private lateinit var uploadLayout: LinearLayout
    private var selectedUri: Uri? = null

    private var leaseId: Int = -1
    private var propertyId: Int = -1

    companion object {
        private const val REQUEST_IMAGE_PICK = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maintenance_request)

        // Get Intent extras safely
        leaseId = intent?.getIntExtra("leaseId", -1) ?: -1
        propertyId = intent?.getIntExtra("propertyId", -1) ?: -1

        if (propertyId == -1) {
            Toast.makeText(this, getString(R.string.error_no_property), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize views
        spinnerCategory = findViewById(R.id.spinnerCategory)
        spinnerUrgency = findViewById(R.id.spinnerUrgency)
        etDescription = findViewById(R.id.etDescription)
        btnSubmit = findViewById(R.id.btnSubmit)
        uploadLayout = findViewById(R.id.uploadContainer)
        ivUpload = findViewById(R.id.ivUpload)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        spinnerCategory.adapter = ArrayAdapter.createFromResource(
            this,
            R.array.maintenance_categories,
            android.R.layout.simple_spinner_dropdown_item
        )

        spinnerUrgency.adapter = ArrayAdapter.createFromResource(
            this,
            R.array.urgency_levels,
            android.R.layout.simple_spinner_dropdown_item
        )

        uploadLayout.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            startActivityForResult(intent, REQUEST_IMAGE_PICK)
        }

        btnSubmit.setOnClickListener { submitMaintenance() }

        findViewById<LinearLayout>(R.id.navDashboard).setOnClickListener {
            startActivity(Intent(this, TenantDashboardActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navMaintenance).setOnClickListener { }
        findViewById<LinearLayout>(R.id.navPayments).setOnClickListener {
            startActivity(Intent(this, UploadProofActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun submitMaintenance() {
        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        val jwt = prefs.getString("jwt", null)
        val tenantId = prefs.getInt("userId", -1)
        val category = spinnerCategory.selectedItem.toString()
        val urgency = spinnerUrgency.selectedItem.toString()
        val description = etDescription.text.toString().trim()

        if (jwt.isNullOrEmpty() || tenantId == -1) {
            Toast.makeText(this, getString(R.string.error_login_required), Toast.LENGTH_SHORT).show()
            return
        }

        if (description.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_description_required), Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)
            val online = isOnline()

            val requestEntity = OfflineRequest(
                tenantId = tenantId,
                propertyId = propertyId,
                category = category,
                urgency = urgency,
                description = description,
                imageUri = selectedUri?.toString(),
                syncStatus = if (online) "synced" else "pending"
            )
            db.offlineDao().insertRequest(requestEntity)

            if (online) {
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
                        tenantPart, propertyPart, descriptionPart, categoryPart, urgencyPart, proofPart
                    )

                    if (!response.isSuccessful) {
                        requestEntity.syncStatus = "pending"
                        db.offlineDao().updateRequest(requestEntity)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    requestEntity.syncStatus = "pending"
                    db.offlineDao().updateRequest(requestEntity)
                }
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@ReportMaintenanceActivity,
                    if (online) getString(R.string.maintenance_submit_success)
                    else getString(R.string.maintenance_saved_offline),
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK) {
            selectedUri = data?.data
            selectedUri?.let {
                ivUpload.setImageURI(it)
                Toast.makeText(this, getString(R.string.photo_selected), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
