package com.rentals.eliterentals

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.rentals.eliterentals.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File

class CaretakerTrackMaintenanceActivity : AppCompatActivity() {

    private val api = RetrofitClient.instance
    private lateinit var jwtToken: String
    private var selectedMaintenance: Maintenance? = null
    private var selectedUri: Uri? = null
    private val REQUEST_IMAGE_PICK = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_caretaker_track_maintenance)

        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        val tvCaretakerName = findViewById<TextView>(R.id.tvCaretakerName)
        val caretakerName = prefs.getString("tenantName", "Caretaker") // Reads saved full name
        tvCaretakerName.text = "Hello, $caretakerName"

        jwtToken = prefs.getString("jwt", "") ?: ""

        findViewById<ImageView>(R.id.settingsIcon).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }


        findViewById<ImageView>(R.id.notificationIcon).setOnClickListener {
            startActivity(Intent(this, MessagesActivity::class.java))
        }


        fetchAssignedTasks()
    }

    private fun fetchAssignedTasks() {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    api.getCaretakerRequests("Bearer $jwtToken") // ✅ use caretaker endpoint
                }

                if (response.isSuccessful) {
                    val tasks = response.body() ?: emptyList()
                    displayTasks(tasks)
                } else {
                    Toast.makeText(
                        this@CaretakerTrackMaintenanceActivity,
                        "Failed to fetch tasks",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@CaretakerTrackMaintenanceActivity,
                    "Error fetching tasks",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Helper function to style status TextView based on status
    private fun applyStatusStyle(statusView: TextView, status: String) {
        val context = statusView.context
        when (status) {
            "Pending" -> {
                statusView.setBackgroundResource(R.drawable.bg_status_pending)
                statusView.text = context.getString(R.string.status_pending)
            }

            "In Progress" -> {
                statusView.setBackgroundResource(R.drawable.bg_status_in_progress)
                statusView.text = context.getString(R.string.status_in_progress)
            }

            "Resolved" -> {
                statusView.setBackgroundResource(R.drawable.bg_status_resolved)
                statusView.text = context.getString(R.string.status_resolved)
            }

            else -> {
                statusView.setBackgroundResource(R.drawable.bg_status_default)
                statusView.text = status
            }
        }

    }

    private fun addComment(maintenance: Maintenance, comment: String) {
        lifecycleScope.launch {
            try {
                val prefs = getSharedPreferences("app", MODE_PRIVATE)
                val userId = prefs.getInt("userId", 0)
                val dto = MaintenanceCommentDto(userId, comment)

                val response = withContext(Dispatchers.IO) {
                    api.addMaintenanceComment(
                        "Bearer $jwtToken",
                        maintenance.maintenanceId,
                        dto
                    )
                }

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@CaretakerTrackMaintenanceActivity,
                        "Comment sent to tenant",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@CaretakerTrackMaintenanceActivity,
                        "Failed to send comment",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@CaretakerTrackMaintenanceActivity,
                    "Error sending comment",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    private fun updateStatus(maintenance: Maintenance, newStatus: String) {
        lifecycleScope.launch {
            try {
                val dto = MaintenanceStatusDto(newStatus)
                val response = withContext(Dispatchers.IO) {
                    api.updateMaintenanceStatus(
                        "Bearer $jwtToken",
                        maintenance.maintenanceId,
                        dto
                    )
                }

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@CaretakerTrackMaintenanceActivity,
                        "Status updated",
                        Toast.LENGTH_SHORT
                    ).show()
                    fetchAssignedTasks()
                } else {
                    Toast.makeText(
                        this@CaretakerTrackMaintenanceActivity,
                        "Failed to update status",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@CaretakerTrackMaintenanceActivity,
                    "Error updating status",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    private fun editPhoto(maintenance: Maintenance) {
        selectedMaintenance = maintenance
        val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }
    private fun displayTasks(tasks: List<Maintenance>) {
        val containerLogs = findViewById<LinearLayout>(R.id.containerLogs)
        containerLogs.removeAllViews()

        tasks.forEach { maintenance ->
            val view = layoutInflater.inflate(R.layout.item_maintenance, containerLogs, false)

            // Set title / description
            view.findViewById<TextView>(R.id.tvTitle).text = maintenance.description

            // Set status TextView
            val statusTextView = view.findViewById<TextView>(R.id.tvStatus)
            statusTextView.text = maintenance.status
            applyStatusStyle(statusTextView, maintenance.status)

            // Status click listener
            statusTextView.setOnClickListener {
                val options = arrayOf("Pending", "In Progress", "Resolved")
                AlertDialog.Builder(this@CaretakerTrackMaintenanceActivity)
                    .setTitle("Update Status")
                    .setItems(options) { _, which ->
                        val newStatus = options[which]

                        // 1. Update local object and UI immediately
                        maintenance.status = newStatus
                        applyStatusStyle(statusTextView, newStatus)

                        // 2. Call backend to save
                        updateStatus(maintenance, newStatus)
                    }
                    .show()
            }

            // Add comment button
            view.findViewById<ImageView>(R.id.ivComment)?.setOnClickListener {
                val input = EditText(this@CaretakerTrackMaintenanceActivity)
                input.hint = "Enter comment for tenant"

                AlertDialog.Builder(this@CaretakerTrackMaintenanceActivity)
                    .setTitle("Add Comment")
                    .setView(input)
                    .setPositiveButton("Send") { _, _ ->
                        val commentText = input.text.toString()
                        if (commentText.isNotBlank()) {
                            addComment(maintenance, commentText)
                        } else {
                            Toast.makeText(this, "Comment cannot be empty", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            // Image click listener
            view.findViewById<ImageView>(R.id.ivImage)?.setOnClickListener {
                editPhoto(maintenance)
            }

            containerLogs.addView(view)
        }

        }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK) {
            selectedUri = data?.data
            selectedMaintenance?.let { maintenance ->
                lifecycleScope.launch {
                    try {
                        val filePath =
                            FileUtils.getPath(
                                this@CaretakerTrackMaintenanceActivity,
                                selectedUri!!
                            )
                        val file = File(filePath!!)
                        val requestFile = RequestBody.create(
                            contentResolver.getType(selectedUri!!)?.toMediaTypeOrNull(),
                            file
                        )
                        val body = MultipartBody.Part.createFormData(
                            "proof",
                            file.name,
                            requestFile
                        )

                        val response = withContext(Dispatchers.IO) {
                            api.updateMaintenanceProof(
                                "Bearer $jwtToken",
                                maintenance.maintenanceId,
                                body
                            )
                        }

                        if (response.isSuccessful) {
                            Toast.makeText(
                                this@CaretakerTrackMaintenanceActivity,
                                "Photo updated",
                                Toast.LENGTH_SHORT
                            ).show()
                            fetchAssignedTasks()
                        } else {
                            Toast.makeText(
                                this@CaretakerTrackMaintenanceActivity,
                                "Failed to update photo",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(
                            this@CaretakerTrackMaintenanceActivity,
                            "Error uploading photo",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
    }

