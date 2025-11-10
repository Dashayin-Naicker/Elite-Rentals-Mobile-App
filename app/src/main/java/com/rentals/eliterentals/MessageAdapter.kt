package com.rentals.eliterentals

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    private val messages: List<MessageDto>,
    private val onReplyClick: (MessageDto) -> Unit
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val senderText: TextView = view.findViewById(R.id.senderText)
        val messageText: TextView = view.findViewById(R.id.messageText)
        val timestampText: TextView = view.findViewById(R.id.timestampText)
        val replyButton: Button = view.findViewById(R.id.replyButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val msg = messages[position]
        val role = SharedPrefs.getUserRole(holder.itemView.context, msg.senderId)
        holder.senderText.text = "From: ${role ?: "User"} (ID: ${msg.senderId})"

        holder.messageText.text = msg.messageText

        // ✅ Format timestamp
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())
        val formattedTime = try {
            outputFormat.format(inputFormat.parse(msg.timestamp ?: "")!!)
        } catch (e: Exception) {
            msg.timestamp ?: ""
        }
        holder.timestampText.text = formattedTime

        holder.replyButton.setOnClickListener {
            onReplyClick(msg)
        }
    }

    override fun getItemCount(): Int = messages.size
}
