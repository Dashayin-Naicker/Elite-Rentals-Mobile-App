package com.rentals.eliterentals

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.rentals.eliterentals.utils.FileUtils
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.math.BigDecimal

class AddEditPropertyActivity : AppCompatActivity() {

    private var editingProperty: PropertyDto? = null
    private var selectedImageFile: File? = null
    private val client = OkHttpClient()
    private lateinit var jwt: String

    // Inputs
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

        // Find views
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

        // Check if editing existing property
        editingProperty = intent.getParcelableExtra("property")
        editingProperty?.let { fillForm(it) }

        btnPickImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, 101)
        }

        btnSave.setOnClickListener { saveProperty() }
    }

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
        spStatus.setSelection(
            if ((p.status?.lowercase() ?: "available") == "available") 0 else 1
        )
    }


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

    private fun saveProperty() {
        val title = etTitle.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val address = etAddress.text.toString().trim()
        val city = etCity.text.toString().trim()
        val province = etProvince.text.toString().trim()
        val country = etCountry.text.toString().trim()
        val rentStr = etRent.text.toString().trim()
        val bedroomsStr = etBedrooms.text.toString().trim()
        val bathroomsStr = etBathrooms.text.toString().trim()
        val parkingType = etParkingType.text.toString().trim()
        val parkingSpotsStr = etParkingSpots.text.toString().trim()
        val petFriendly = cbPetFriendly.isChecked
        val status = spStatus.selectedItem.toString()

        if (title.isEmpty() || address.isEmpty() || rentStr.isEmpty()) {
            Toast.makeText(this, "Please fill in required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val rent = rentStr.toDoubleOrNull() ?: 0.0
        val bedrooms = bedroomsStr.toIntOrNull() ?: 0
        val bathrooms = bathroomsStr.toIntOrNull() ?: 0
        val parkingSpots = parkingSpotsStr.toIntOrNull() ?: 0

        if (rent <= 0) {
            Toast.makeText(this, "Enter a valid rent amount", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedImageFile == null && editingProperty == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
            return
        }

        val property = Property(
            title = title,
            description = description.ifEmpty { "No description" },
            address = address,
            city = city.ifEmpty { "Unknown" },
            province = province.ifEmpty { "Unknown" },
            country = country.ifEmpty { "South Africa" },
            rentAmount = BigDecimal(rent),
            numOfBedrooms = bedrooms,
            numOfBathrooms = bathrooms,
            parkingType = parkingType.ifEmpty { "None" },
            numOfParkingSpots = parkingSpots,
            petFriendly = petFriendly,
            status = status
        )

        if (editingProperty != null) {
            updateProperty(editingProperty!!.propertyId, property)
        } else {
            uploadPropertyWithImage(property, selectedImageFile!!)
        }
    }

    private fun uploadPropertyWithImage(property: Property, imageFile: File) {
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
            .url("http://10.0.2.2:5263/api/Property")
            .header("Authorization", "Bearer $jwt")
            .post(builder.build())
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(this@AddEditPropertyActivity, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                Handler(Looper.getMainLooper()).post {
                    if (response.isSuccessful) {
                        Toast.makeText(this@AddEditPropertyActivity, "Property saved successfully!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Log.e("PropertyUpload", "Error ${response.code}")
                        Toast.makeText(this@AddEditPropertyActivity, "Error ${response.code}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    private fun updateProperty(propertyId: Int, property: Property) {
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
            .url("http://10.0.2.2:5263/api/Property/$propertyId")
            .header("Authorization", "Bearer $jwt")
            .put(builder.build())
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(this@AddEditPropertyActivity, "Update failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                Handler(Looper.getMainLooper()).post {
                    if (response.isSuccessful) {
                        Toast.makeText(this@AddEditPropertyActivity, "Property updated successfully!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Log.e("PropertyUpdate", "Error ${response.code}")
                        Toast.makeText(this@AddEditPropertyActivity, "Error ${response.code}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }
}
