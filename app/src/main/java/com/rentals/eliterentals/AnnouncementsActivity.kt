package com.rentals.eliterentals

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.widget.EditText
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import android.widget.Spinner

class AnnouncementsActivity : BaseActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AnnouncementAdapter
    private val announcements = mutableListOf<MessageDto>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_announcements)

        recyclerView = findViewById(R.id.recyclerViewAnnouncements)
        adapter = AnnouncementAdapter(announcements)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val role = getSharedPreferences("app", MODE_PRIVATE)
            .getString("role", "Tenant")


        if (role == "Admin" || role == "PropertyManager") {
            val btnPost = findViewById<Button>(R.id.btnPostAnnouncement)
            btnPost.visibility = View.VISIBLE
            btnPost.setOnClickListener { openAnnouncementDialog() }
        }


        fetchAnnouncements()
    }

    private fun openAnnouncementDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_announcement, null)
        val edtText = dialogView.findViewById<EditText>(R.id.edtAnnouncementText)
        val spinnerRole = dialogView.findViewById<Spinner>(R.id.spinnerRole)

        val roles = listOf("All Users", "Caretaker", "Tenant", "Property Manager", "Admin")
        val roleAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRole.adapter = roleAdapter

        AlertDialog.Builder(this)
            .setTitle("New Announcement")
            .setView(dialogView)
            .setPositiveButton("Send") { _, _ ->
                val text = edtText.text.toString().trim()
                val selectedRole = spinnerRole.selectedItem.toString()
                val role = if (selectedRole == "All Users") null else selectedRole
                sendAnnouncement(text, role)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun sendAnnouncement(text: String, role: String?) {
        val token = SharedPrefs.getToken(this)
        val senderId = SharedPrefs.getUserId(this)

        val msg = MessageDto(
            senderId = senderId,
            messageText = text,
            isBroadcast = true,
            targetRole = role
        )

        RetrofitClient.instance.sendBroadcast("Bearer $token", msg)
            .enqueue(object : Callback<MessageDto> {
                override fun onResponse(call: Call<MessageDto>, response: Response<MessageDto>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@AnnouncementsActivity, "Announcement sent", Toast.LENGTH_SHORT).show()
                        fetchAnnouncements()
                    } else {
                        Toast.makeText(this@AnnouncementsActivity, "Failed", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<MessageDto>, t: Throwable) {
                    Toast.makeText(this@AnnouncementsActivity, "Network error", Toast.LENGTH_SHORT).show()
                }
            })
    }



    private fun fetchAnnouncements() {
        val token = SharedPrefs.getToken(this)
        val userId = SharedPrefs.getUserId(this)

        RetrofitClient.instance.getAnnouncements("Bearer $token", userId)
            .enqueue(object : Callback<List<MessageDto>> {
                override fun onResponse(call: Call<List<MessageDto>>, response: Response<List<MessageDto>>) {
                    if (response.isSuccessful) {
                        announcements.clear()
                        announcements.addAll(response.body() ?: emptyList())
                        adapter.notifyDataSetChanged()
                    } else {
                        Toast.makeText(
                            this@AnnouncementsActivity,
                            getString(R.string.error_loading_announcements),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<List<MessageDto>>, t: Throwable) {
                    Toast.makeText(
                        this@AnnouncementsActivity,
                        getString(R.string.error_network),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
}
