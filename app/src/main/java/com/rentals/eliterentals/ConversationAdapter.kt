package com.rentals.eliterentals

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class ConversationAdapter(
    private val messages: List<MessageDto>,
    private val currentUserId: Int
) : RecyclerView.Adapter<ConversationAdapter.MessageViewHolder>() {

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val bubble: View = view.findViewById(R.id.messageBubble)
        val messageText: TextView = view.findViewById(R.id.messageText)
        val timestampText: TextView = view.findViewById(R.id.timestampText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val msg = messages[position]
        holder.messageText.text = msg.messageText

        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())
        val formattedTime = try {
            outputFormat.format(inputFormat.parse(msg.timestamp ?: "")!!)
        } catch (e: Exception) {
            msg.timestamp ?: ""
        }
        holder.timestampText.text = formattedTime

        val isSentByMe = msg.senderId == currentUserId
        val params = holder.bubble.layoutParams as ViewGroup.MarginLayoutParams
        params.marginStart = if (isSentByMe) 80 else 16
        params.marginEnd = if (isSentByMe) 16 else 80
        holder.bubble.layoutParams = params

        holder.bubble.setBackgroundResource(
            if (isSentByMe) R.drawable.bubble_background_me else R.drawable.bubble_background
        )
    }

    override fun getItemCount(): Int = messages.size
}
