package com.rentals.eliterentals

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.core.content.ContextCompat



class ReplyActivity : AppCompatActivity() {
    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ConversationAdapter
    private val messages = mutableListOf<MessageDto>()

    private var receiverId: Int = 0
    private var currentUserId: Int = 0
    private lateinit var token: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reply)

        receiverId = intent.getIntExtra("receiverId", 0)
        currentUserId = SharedPrefs.getUserId(this)
        token = SharedPrefs.getToken(this)

        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)
        recyclerView = findViewById(R.id.recyclerViewConversation)

        adapter = ConversationAdapter(messages, currentUserId)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        recyclerView.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL).apply {
                setDrawable(ContextCompat.getDrawable(this@ReplyActivity, R.drawable.chat_spacing)!!)
            }
        )

        val divider = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        val spacingDrawable = ContextCompat.getDrawable(this, R.drawable.chat_spacing)
        if (spacingDrawable != null) {
            divider.setDrawable(spacingDrawable)
            recyclerView.addItemDecoration(divider)
        }




        sendButton.setOnClickListener {
            val text = messageInput.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
            }
        }

        loadConversation()
    }

    private fun loadConversation() {
        RetrofitClient.instance.getConversation("Bearer $token", currentUserId, receiverId)
            .enqueue(object : Callback<List<MessageDto>> {
                override fun onResponse(call: Call<List<MessageDto>>, response: Response<List<MessageDto>>) {
                    if (response.isSuccessful) {
                        messages.clear()
                        messages.addAll(response.body() ?: emptyList())
                        adapter.notifyDataSetChanged()
                        recyclerView.scrollToPosition(messages.size - 1)
                    } else {
                        Toast.makeText(this@ReplyActivity, "Failed to load conversation", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<List<MessageDto>>, t: Throwable) {
                    Toast.makeText(this@ReplyActivity, "Network error", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun sendMessage(text: String) {
        val msg = MessageDto(
            messageId = null,
            senderId = currentUserId,
            receiverId = receiverId,
            messageText = text,
            timestamp = null,
            isChatbot = false,
            isBroadcast = false,
            targetRole = null,
            isEscalated = false,
            language = "en"
        )

        RetrofitClient.instance.sendMessage("Bearer $token", msg)
            .enqueue(object : Callback<MessageDto> {
                override fun onResponse(call: Call<MessageDto>, response: Response<MessageDto>) {
                    if (response.isSuccessful) {
                        messageInput.text.clear()
                        loadConversation() // Refresh after sending
                    }
                }

                override fun onFailure(call: Call<MessageDto>, t: Throwable) {
                    Toast.makeText(this@ReplyActivity, "Failed to send", Toast.LENGTH_SHORT).show()
                }
            })
    }

}
