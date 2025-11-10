package com.rentals.eliterentals

import android.content.Intent
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
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.math.BigDecimal

class AddEditPropertyActivity : AppCompatActivity() {

    private var editingProperty: PropertyDto? = null
    private var selectedImageFile: File? = null
    private val client = OkHttpClient()
    private lateinit var jwt: String

    // UI
    private lateinit var etTitle: EditText
    private lateinit var etDescription: EditText
    private lateinit var etAddress: EditText
    private lateinit var etCity: EditText
    private lateinit var etProvince: EditText
    private lateinit var etCountry: EditText
    private lateinit var etRent: EditText
    private lateinit var etBedrooms: EditText
    private lateinit var etBathrooms: EditText
    private lateinit var etParkingType: EditText
    private lateinit var etParkingSpots: EditText
    private lateinit var cbPetFriendly: CheckBox
    private lateinit var spStatus: Spinner
    private lateinit var btnSave: Button
    private lateinit var btnPickImage: Button
    private lateinit var ivPreview: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_property)

        // 🔹 Top bar back -> Dashboard
        findViewById<ImageView>(R.id.ic_back)?.setOnClickListener {
            goToDashboard()
        }

        // 🔹 Bottom navbar listeners (including Dashboard)
        findViewById<ImageView>(R.id.navDashboard)?.setOnClickListener { goToDashboard() }

        findViewById<ImageView>(R.id.navManageProperties)?.setOnClickListener {
            // dedicated Properties list screen
            startActivity(Intent(this, PropertyListActivity::class.java))
            finish()
        }

        findViewById<ImageView>(R.id.navManageTenants)?.setOnClickListener {
            // open the PM main screen (Tenants available from there)
            startActivity(Intent(this, MainPmActivity::class.java))
            finish()
        }

        findViewById<ImageView>(R.id.navAssignLeases)?.setOnClickListener {
            startActivity(Intent(this, AssignLeaseActivity::class.java))
            finish()
        }

        findViewById<ImageView>(R.id.navAssignMaintenance)?.setOnClickListener {
            startActivity(Intent(this, CaretakerTrackMaintenanceActivity::class.java))
            finish()
        }

        findViewById<ImageView>(R.id.navRegisterTenant)?.setOnClickListener {
            startActivity(Intent(this, RegisterTenantActivity::class.java))
            finish()
        }

        findViewById<ImageView>(R.id.navGenerateReport)?.setOnClickListener {
            Toast.makeText(this, "Coming Soon", Toast.LENGTH_SHORT).show()
        }

        // 🔹 Form views
        etTitle = findViewById(R.id.etTitle)
        etDescription = findViewById(R.id.etDescription)
        etAddress = findViewById(R.id.etAddress)
        etCity = findViewById(R.id.etCity)
        etProvince = findViewById(R.id.etProvince)
        etCountry = findViewById(R.id.etCountry)
        etRent = findViewById(R.id.etRent)
        etBedrooms = findViewById(R.id.etBedrooms)
        etBathrooms = findViewById(R.id.etBathrooms)
        etParkingType = findViewById(R.id.etParkingType)
        etParkingSpots = findViewById(R.id.etParkingSpots)
        cbPetFriendly = findViewById(R.id.cbPetFriendly)
        spStatus = findViewById(R.id.spStatus)
        btnSave = findViewById(R.id.btnSave)
        btnPickImage = findViewById(R.id.btnPickImage)
        ivPreview = findViewById(R.id.ivPreview)

        jwt = getSharedPreferences("app", MODE_PRIVATE).getString("jwt", "") ?: ""

        // If editing, prefill
        editingProperty = intent.getParcelableExtra("property")
        editingProperty?.let { fillForm(it) }

        btnPickImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            startActivityForResult(intent, 101)
        }

        btnSave.setOnClickListener { saveProperty() }
    }

    private fun goToDashboard() {
        val intent = Intent(this, MainPmActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
        finish()
    }

    // 🔹 Prefill when editing
    private fun fillForm(p: PropertyDto) {
        etTitle.setText(p.title ?: "")
        etDescription.setText(p.description ?: "")
        etAddress.setText(p.address ?: "")
        etCity.setText(p.city ?: "")
        etProvince.setText(p.province ?: "")
        etCountry.setText(p.country ?: "")
        etRent.setText(p.rentAmount?.toString() ?: "")
        etBedrooms.setText(p.numOfBedrooms?.toString() ?: "")
        etBathrooms.setText(p.numOfBathrooms?.toString() ?: "")
        etParkingType.setText(p.parkingType ?: "")
        etParkingSpots.setText(p.numOfParkingSpots?.toString() ?: "")
        cbPetFriendly.isChecked = p.petFriendly ?: false
        spStatus.setSelection(if ((p.status?.lowercase() ?: "available") == "available") 0 else 1)
    }

    // 🔹 Handle image picker
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101 && resultCode == RESULT_OK) {
            val uri: Uri? = data?.data
            uri?.let {
                val path = FileUtils.getPath(this, it)
                if (path != null) {
                    selectedImageFile = File(path)
                    ivPreview.setImageURI(uri)
                }
            }
        }
    }

    // 🔹 Save or update property
    private fun saveProperty() {
        val title = etTitle.text.toString().trim()
        val address = etAddress.text.toString().trim()
        val rentStr = etRent.text.toString().trim()

        if (title.isEmpty() || address.isEmpty() || rentStr.isEmpty()) {
            Toast.makeText(this, getString(R.string.fill_fields), Toast.LENGTH_SHORT).show()
            return
        }

        val rent = rentStr.toDoubleOrNull() ?: 0.0
        if (rent <= 0) {
            Toast.makeText(this, getString(R.string.enter_valid_rent), Toast.LENGTH_SHORT).show()

            return
        }

        val property = Property(
            title = title,
            description = etDescription.text.toString().ifEmpty { "No description" },
            address = address,
            city = etCity.text.toString().ifEmpty { "Unknown" },
            province = etProvince.text.toString().ifEmpty { "Unknown" },
            country = etCountry.text.toString().ifEmpty { "South Africa" },
            rentAmount = BigDecimal(rent),
            numOfBedrooms = etBedrooms.text.toString().toIntOrNull() ?: 0,
            numOfBathrooms = etBathrooms.text.toString().toIntOrNull() ?: 0,
            parkingType = etParkingType.text.toString().ifEmpty { "None" },
            numOfParkingSpots = etParkingSpots.text.toString().toIntOrNull() ?: 0,
            petFriendly = cbPetFriendly.isChecked,
            status = spStatus.selectedItem.toString()
        )

        if (editingProperty != null) {
            updateProperty(editingProperty!!.propertyId, property)
        } else if (selectedImageFile != null) {
            uploadPropertyWithImage(property, selectedImageFile!!)
        } else {
            Toast.makeText(this, getString(R.string.select_image), Toast.LENGTH_SHORT).show()

        }
    }

    // 🔹 Upload property with image (POST)
    private fun uploadPropertyWithImage(property: Property, imageFile: File) {
        lifecycleScope.launch {
            try {
                val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("title", property.title)
                    .addFormDataPart("description", property.description)
                    .addFormDataPart("address", property.address)
                    .addFormDataPart("city", property.city)
                    .addFormDataPart("province", property.province)
                    .addFormDataPart("country", property.country)
                    .addFormDataPart("rentAmount", property.rentAmount.toPlainString())
                    .addFormDataPart("numOfBedrooms", property.numOfBedrooms.toString())
                    .addFormDataPart("numOfBathrooms", property.numOfBathrooms.toString())
                    .addFormDataPart("parkingType", property.parkingType)
                    .addFormDataPart("numOfParkingSpots", property.numOfParkingSpots.toString())
                    .addFormDataPart("petFriendly", property.petFriendly.toString())
                    .addFormDataPart("status", property.status)

                val imageRequestBody = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                builder.addFormDataPart("image", imageFile.name, imageRequestBody)

                val request = Request.Builder()
                    .url("https://eliterentalsapi-czckh7fadmgbgtgf.southafricanorth-01.azurewebsites.net/api/Property")
                    .header("Authorization", "Bearer $jwt")
                    .post(builder.build())
                    .build()

                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }

                if (response.isSuccessful) {
                    Toast.makeText(this@AddEditPropertyActivity, getString(R.string.property_saved), Toast.LENGTH_SHORT).show()

                    goToDashboard()
                } else {
                    Toast.makeText(this@AddEditPropertyActivity, getString(R.string.property_error_code, response.code), Toast.LENGTH_LONG).show()

                }

            } catch (e: IOException) {
                Toast.makeText(this@AddEditPropertyActivity, getString(R.string.upload_failed_with_message, e.message), Toast.LENGTH_LONG).show()

            }
        }
    }

    // 🔹 Update property (PUT)
    private fun updateProperty(propertyId: Int, property: Property) {
        lifecycleScope.launch {
            try {
                val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("title", property.title)
                    .addFormDataPart("description", property.description)
                    .addFormDataPart("address", property.address)
                    .addFormDataPart("city", property.city)
                    .addFormDataPart("province", property.province)
                    .addFormDataPart("country", property.country)
                    .addFormDataPart("rentAmount", property.rentAmount.toPlainString())
                    .addFormDataPart("numOfBedrooms", property.numOfBedrooms.toString())
                    .addFormDataPart("numOfBathrooms", property.numOfBathrooms.toString())
                    .addFormDataPart("parkingType", property.parkingType)
                    .addFormDataPart("numOfParkingSpots", property.numOfParkingSpots.toString())
                    .addFormDataPart("petFriendly", property.petFriendly.toString())
                    .addFormDataPart("status", property.status)

                selectedImageFile?.let {
                    val imageRequestBody = it.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    builder.addFormDataPart("image", it.name, imageRequestBody)
                }

                val request = Request.Builder()
                    .url("https://eliterentalsapi-czckh7fadmgbgtgf.southafricanorth-01.azurewebsites.net/api/Property/$propertyId")
                    .header("Authorization", "Bearer $jwt")
                    .put(builder.build())
                    .build()

                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }

                if (response.isSuccessful) {
                    Toast.makeText(this@AddEditPropertyActivity, getString(R.string.property_updated), Toast.LENGTH_SHORT).show()

                    goToDashboard()
                } else {
                    Toast.makeText(this@AddEditPropertyActivity, getString(R.string.property_error_code, response.code), Toast.LENGTH_LONG).show()

                }

            } catch (e: IOException) {
                Toast.makeText(this@AddEditPropertyActivity, getString(R.string.update_failed_with_message, e.message), Toast.LENGTH_LONG).show()

            }
        }
    }
}
