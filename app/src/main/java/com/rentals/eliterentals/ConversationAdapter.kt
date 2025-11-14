package com.rentals.eliterentals

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class ConversationAdapter(
    private val messages: List<MessageDto>,
    private val currentUserId: Int
) : RecyclerView.Adapter<ConversationAdapter.MessageViewHolder>() {

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val container: LinearLayout = view.findViewById(R.id.messageContainer)
        val bubble: LinearLayout = view.findViewById(R.id.messageBubble)
        val messageText: TextView = view.findViewById(R.id.messageText)
        val timestampText: TextView = view.findViewById(R.id.timestampText)
        val profileIcon: ImageView = view.findViewById(R.id.profileIcon)
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

        // Adjust bubble margins
        val params = holder.bubble.layoutParams as ViewGroup.MarginLayoutParams
        params.marginStart = if (isSentByMe) 80 else 16
        params.marginEnd = if (isSentByMe) 16 else 80
        holder.bubble.layoutParams = params

        // Set bubble background and icon based on sender
        holder.bubble.setBackgroundResource(
            if (isSentByMe) R.drawable.chat_bubble_me else R.drawable.chat_bubble_incoming
        )
        holder.profileIcon.setImageResource(
            if (isSentByMe) R.drawable.ic_my_icon else R.drawable.ic_user_placeholder
        )

        // Set message and timestamp text color
        if (isSentByMe) {
            holder.messageText.setTextColor(holder.itemView.context.getColor(R.color.textPrimaryOutgoing)) // White
            holder.timestampText.setTextColor(holder.itemView.context.getColor(R.color.textPrimaryOutgoing)) // White
        } else {
            holder.messageText.setTextColor(holder.itemView.context.getColor(R.color.textPrimaryIncoming)) // Dark
            holder.timestampText.setTextColor(holder.itemView.context.getColor(R.color.textPrimaryIncoming)) // Dark
        }

        // Align container and swap icon position
        holder.container.gravity = if (isSentByMe) Gravity.END else Gravity.START
        holder.container.removeAllViews()
        if (isSentByMe) {
            holder.container.addView(holder.bubble)
            holder.container.addView(holder.profileIcon)
        } else {
            holder.container.addView(holder.profileIcon)
            holder.container.addView(holder.bubble)
        }
    }

    override fun getItemCount(): Int = messages.size
}
