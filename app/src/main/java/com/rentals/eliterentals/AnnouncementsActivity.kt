package com.rentals.eliterentals

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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

        fetchAnnouncements()
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
