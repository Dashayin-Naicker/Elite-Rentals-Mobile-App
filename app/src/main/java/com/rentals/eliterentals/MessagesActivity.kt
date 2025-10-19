package com.rentals.eliterentals

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Call
import android.widget.Button


class MessagesActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MessageAdapter
    private val messages = mutableListOf<MessageDto>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)

        recyclerView = findViewById(R.id.recyclerViewMessages)
        val btnViewAnnouncements = findViewById<Button>(R.id.btnViewAnnouncements)

        adapter = MessageAdapter(messages) { message ->
            val intent = Intent(this, ReplyActivity::class.java)
            intent.putExtra("receiverId", message.senderId)
            startActivity(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnViewAnnouncements.setOnClickListener {
            val intent = Intent(this, AnnouncementsActivity::class.java)
            startActivity(intent)
        }

        fetchInbox()
    }


    private fun fetchInbox() {
        val token = SharedPrefs.getToken(this)
        val userId = SharedPrefs.getUserId(this)

        RetrofitClient.instance.getInboxMessages("Bearer $token", userId)
            .enqueue(object : Callback<List<MessageDto>> {
                override fun onResponse(call: Call<List<MessageDto>>, response: Response<List<MessageDto>>) {
                    if (response.isSuccessful) {
                        val allMessages = response.body() ?: emptyList()

                        val latestMessages = allMessages
                            .groupBy { it.senderId }
                            .mapNotNull { (_, msgs) -> msgs.maxByOrNull { it.timestamp ?: "" } }

                        messages.clear()
                        messages.addAll(latestMessages.sortedByDescending { it.timestamp })

                        fetchSenderRoles(token)
                    }
                }

                override fun onFailure(call: Call<List<MessageDto>>, t: Throwable) {
                    Toast.makeText(this@MessagesActivity, "Network error", Toast.LENGTH_SHORT).show()
                }
            })
    }


    private val userMap = mutableMapOf<Int, UserDto>()

    private fun fetchSenderRoles(token: String) {
        val uniqueSenderIds = messages.map { it.senderId }.distinct()

        uniqueSenderIds.forEach { senderId ->
            RetrofitClient.instance.getUserById("Bearer $token", senderId)
                .enqueue(object : Callback<UserDto> {
                    override fun onResponse(call: Call<UserDto>, response: Response<UserDto>) {
                        if (response.isSuccessful) {
                            response.body()?.let { user ->
                                userMap[user.userId] = user
                                adapter.notifyDataSetChanged()
                            }
                        }
                    }

                    override fun onFailure(call: Call<UserDto>, t: Throwable) {
                        Log.e("MessagesActivity", "Failed to fetch user $senderId")
                    }
                })
        }
    }


}

