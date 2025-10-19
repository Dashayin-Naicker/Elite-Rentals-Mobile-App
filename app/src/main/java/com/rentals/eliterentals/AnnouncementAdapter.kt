package com.rentals.eliterentals

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class AnnouncementAdapter(private val items: List<MessageDto>) :
    RecyclerView.Adapter<AnnouncementAdapter.AnnouncementViewHolder>() {

    class AnnouncementViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageText: TextView = view.findViewById(R.id.announcementText)
        val roleText: TextView = view.findViewById(R.id.roleText)
        val timestampText: TextView = view.findViewById(R.id.timestampText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnnouncementViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_announcement, parent, false)
        return AnnouncementViewHolder(view)
    }

    override fun onBindViewHolder(holder: AnnouncementViewHolder, position: Int) {
        val msg = items[position]
        holder.messageText.text = msg.messageText
        holder.roleText.text = if (msg.targetRole == null) "For All Users" else "For ${msg.targetRole}"

        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())
        val formattedTime = try {
            outputFormat.format(inputFormat.parse(msg.timestamp ?: "")!!)
        } catch (e: Exception) {
            msg.timestamp ?: ""
        }
        holder.timestampText.text = formattedTime
    }

    override fun getItemCount(): Int = items.size
}
