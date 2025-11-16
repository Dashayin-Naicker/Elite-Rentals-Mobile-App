package com.rentals.eliterentals

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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
import java.io.FileOutputStream
import java.io.InputStream
import java.math.BigDecimal
import java.util.*

class AddEditPropertyActivity : AppCompatActivity() {

    // UI fields (keep IDs consistent with your layouts)
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
    // Gallery of selected images
    private lateinit var rvSelectedImages: RecyclerView

    private val client = OkHttpClient()
    private var jwt: String = ""

    // Editing property
    private var editingProperty: PropertyDto? = null

    // Local image files selected by user (will be uploaded)
    private val selectedImageFiles = mutableListOf<File>()
    private val selectedImageUris = mutableListOf<Uri>()

    // Existing remote image URLs (when editing). Display-only unless you add a delete endpoint.
    private val existingImageUrls = mutableListOf<String>()

    private lateinit var imageAdapter: SelectedImageAdapter

    companion object {
        private const val REQUEST_CODE_PICK_IMAGES = 101
        private const val TAG = "AddEditProperty"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_property)

        // Topbar/back handlers (if present in layout)
        findViewById<ImageView?>(R.id.ic_back)?.setOnClickListener { goToDashboard() }
        findViewById<ImageView?>(R.id.navDashboard)?.setOnClickListener { goToDashboard() }

        // REMOVE YOUR OLD ADAPTER CODE — IT WAS WRONG

        // init UI
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

        rvSelectedImages = findViewById(R.id.rvSelectedImages)

        jwt = getSharedPreferences("app", Context.MODE_PRIVATE).getString("jwt", "") ?: ""

        // THIS is the correct adapter
        imageAdapter = SelectedImageAdapter(
            ctx = this,
            selectedUris = selectedImageUris,
            existingUrls = existingImageUrls,
            onRemoveLocal = { position -> removeLocalImage(position) },
            onRemoveExisting = { position -> removeExistingImage(position) }
        )

        rvSelectedImages.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvSelectedImages.adapter = imageAdapter


        // If editing, prefill values and existing images
        editingProperty = intent.getParcelableExtra("property")
        editingProperty?.let { prefillForm(it) }

        // pick images (multiple)
        btnPickImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            startActivityForResult(Intent.createChooser(intent, "Select images"), REQUEST_CODE_PICK_IMAGES)
        }

        btnSave.setOnClickListener { saveProperty() }
    }

    private fun goToDashboard() {
        val intent = Intent(this, MainPmActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
        finish()
    }

    private fun prefillForm(p: PropertyDto) {
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
        // set spinner selection by matching status string (simple)
        val status = p.status ?: "Available"
        val statusAdapter = spStatus.adapter
        if (statusAdapter != null) {
            for (i in 0 until statusAdapter.count) {
                if (statusAdapter.getItem(i)?.toString()?.equals(status, true) == true) {
                    spStatus.setSelection(i)
                    break
                }
            }
        }

        // load existing image URLs for preview
        p.imageUrlList()?.let { urls ->
            existingImageUrls.addAll(urls)
            imageAdapter.notifyDataSetChanged()
        }
    }

    // Helper extension to read imageUrls if your DTO provides multiple image urls (PropertyReadDto used imageUrls)
    private fun PropertyDto.imageUrlList(): List<String>? {
        // The earlier DTO exposed imageUrls list on the server. In this client DTO we constructed imageUrl as a single endpoint.
        // But your PropertyDto or PropertyReadDto coming from API might include a dedicated list field.
        // Try to extract possible list if present via reflection / alternate field - fallback to single constructed url.
        // If your PropertyDto has imageUrls: List<String>, change the DTO to include that and return it here.
        return try {
            val field = this::class.java.getDeclaredField("imageUrls")
            field.isAccessible = true
            val value = field.get(this)
            if (value is List<*>) {
                value.filterIsInstance<String>()
            } else null
        } catch (ex: Exception) {
            // fallback: single constructed URL when propertyId present (old behaviour)
            if (this.propertyId != 0) {
                listOf("https://eliterentalsapi-czckh7fadmgbgtgf.southafricanorth-01.azurewebsites.net/api/Property/${this.propertyId}/image")
            } else null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_IMAGES && resultCode == Activity.RESULT_OK) {
            if (data == null) return
            // multiple selection
            val clip = data.clipData
            if (clip != null) {
                for (i in 0 until clip.itemCount) {
                    val uri = clip.getItemAt(i).uri
                    addSelectedUri(uri)
                }
            } else {
                data.data?.let { addSelectedUri(it) }
            }
        }
    }

    private fun addSelectedUri(uri: Uri) {
        // convert to File (try FileUtils first; if not available, copy to cache)
        lifecycleScope.launch {
            val file = withContext(Dispatchers.IO) {
                try {
                    FileUtils.getPath(this@AddEditPropertyActivity, uri)?.let { File(it) }
                } catch (ex: Throwable) {
                    null
                } ?: copyUriToTempFile(uri)
            }
            if (file != null) {
                selectedImageFiles.add(file)
            }
            selectedImageUris.add(uri)
            imageAdapter.notifyDataSetChanged()
        }
    }

    private fun removeLocalImage(position: Int) {
        // position corresponds to local images first + existing urls after (adapter separates)
        // Our adapter exposes callback only for local images (we pass the correct index)
        if (position in selectedImageFiles.indices) {
            selectedImageFiles.removeAt(position)
        }
        if (position in selectedImageUris.indices) {
            selectedImageUris.removeAt(position)
        }
        imageAdapter.notifyDataSetChanged()
    }

    private fun removeExistingImage(position: Int) {
        // NOTE: server currently doesn't support deleting individual images.
        // This will only remove from UI; to remove server-side you need a dedicated API endpoint.
        if (position in existingImageUrls.indices) {
            existingImageUrls.removeAt(position)
            Toast.makeText(this, "Removed from preview (server deletion not implemented).", Toast.LENGTH_SHORT).show()
            imageAdapter.notifyDataSetChanged()
        }
    }

    // copy URI content to a temp file in cache and return File
    private fun copyUriToTempFile(uri: Uri): File? {
        return try {
            val resolver = contentResolver
            val name = queryFileName(resolver, uri) ?: "img_${System.currentTimeMillis()}.jpg"
            val ext = name.substringAfterLast('.', "jpg")
            val outFile = File(cacheDir, "upload_${UUID.randomUUID()}.$ext")
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(outFile).use { out ->
                    copyStream(input, out)
                }
            }
            outFile
        } catch (ex: Exception) {
            Log.w(TAG, "copyUriToTempFile failed: ${ex.message}")
            null
        }
    }

    private fun queryFileName(resolver: ContentResolver, uri: Uri): String? {
        return try {
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun copyStream(input: InputStream, out: FileOutputStream) {
        val buffer = ByteArray(8 * 1024)
        var read: Int
        while (input.read(buffer).also { read = it } != -1) {
            out.write(buffer, 0, read)
        }
    }

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
            status = spStatus.selectedItem?.toString() ?: "Available"
        )

        if (editingProperty != null) {
            updateProperty(editingProperty!!.propertyId, property)
        } else {
            // create
            uploadPropertyWithImages(property, selectedImageFiles)
        }
    }

    // Upload multiple images as repeated "Images" parts (POST)
    private fun uploadPropertyWithImages(property: Property, images: List<File>) {
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

                images.forEachIndexed { idx, file ->
                    val mime = contentResolver.getType(Uri.fromFile(file)) ?: "image/jpeg"
                    val reqBody = file.asRequestBody(mime.toMediaTypeOrNull())
                    // The form field name must match API: "Images" repeated for List<IFormFile>
                    builder.addFormDataPart("Images", file.name, reqBody)
                }

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
            } catch (e: Exception) {
                Toast.makeText(this@AddEditPropertyActivity, getString(R.string.upload_failed_with_message, e.message), Toast.LENGTH_LONG).show()
            }
        }
    }

    // Update property (PUT) - can attach new Images. Existing images remain on server unless you implement delete endpoint.
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

                // Add new images (server appends)
                selectedImageFiles.forEach { file ->
                    val mime = contentResolver.getType(Uri.fromFile(file)) ?: "image/jpeg"
                    val reqBody = file.asRequestBody(mime.toMediaTypeOrNull())
                    builder.addFormDataPart("Images", file.name, reqBody)
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
            } catch (e: Exception) {
                Toast.makeText(this@AddEditPropertyActivity, getString(R.string.update_failed_with_message, e.message), Toast.LENGTH_LONG).show()
            }
        }
    }

    /* --------------------------
       RecyclerView adapter for image previews
       - Shows local images (selectedImageUris) with a remove button
       - Shows existing remote URLs (readonly remove with warning)
       -------------------------- */
    private class SelectedImageAdapter(
        private val ctx: Context,
        private val selectedUris: List<Uri>,
        private val existingUrls: List<String>,
        private val onRemoveLocal: (Int) -> Unit,
        private val onRemoveExisting: (Int) -> Unit
    ) : RecyclerView.Adapter<SelectedImageAdapter.ImageVH>() {

        override fun getItemCount(): Int = selectedUris.size + existingUrls.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageVH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_selected_image, parent, false)
            return ImageVH(v)
        }

        override fun onBindViewHolder(holder: ImageVH, position: Int) {
            if (position < selectedUris.size) {
                val uri = selectedUris[position]
                holder.removeBtn.visibility = View.VISIBLE
                holder.removeBtn.setOnClickListener { onRemoveLocal(position) }
                Glide.with(ctx).load(uri).centerCrop().into(holder.iv)
            } else {
                val idx = position - selectedUris.size
                val url = existingUrls[idx]
                holder.removeBtn.visibility = View.VISIBLE
                holder.removeBtn.setOnClickListener { onRemoveExisting(idx) }
                Glide.with(ctx).load(url).centerCrop().into(holder.iv)
            }
        }

        class ImageVH(view: View) : RecyclerView.ViewHolder(view) {
            val iv: ImageView = view.findViewById(R.id.imgSelected)
            val removeBtn: TextView = view.findViewById(R.id.btnRemove)
        }
    }


}
