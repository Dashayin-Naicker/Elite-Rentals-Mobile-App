package com.rentals.eliterentals

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders

class MaintenanceAdapter(
    private val items: List<Maintenance>,
    private val jwtToken: String // Add token here
) : RecyclerView.Adapter<MaintenanceAdapter.MaintenanceViewHolder>() {

    inner class MaintenanceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvTitle)
        val status: TextView = view.findViewById(R.id.tvStatus)
        val loggedTime: TextView = view.findViewById(R.id.tvDate)
        val ivImage: ImageView = view.findViewById(R.id.ivImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MaintenanceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_maintenance, parent, false)
        return MaintenanceViewHolder(view)
    }

    override fun onBindViewHolder(holder: MaintenanceViewHolder, position: Int) {
        val item = items[position]

        holder.title.text = item.description
        holder.status.text = item.status
        holder.loggedTime.text = "Logged at: ${item.createdAt}"

        // Color-code status
        when (item.status) {
            "Pending" -> holder.status.setBackgroundResource(R.drawable.bg_status_pending)
            "In Progress" -> holder.status.setBackgroundResource(R.drawable.bg_status_in_progress)
            "Resolved" -> holder.status.setBackgroundResource(R.drawable.bg_status_resolved)
        }

        // Build GlideUrl with Authorization header
        val imageUrl = item.imageUrl?.let {
            GlideUrl(
                it,
                LazyHeaders.Builder()
                    .addHeader("Authorization", "Bearer $jwtToken")
                    .build()
            )
        }

        // Load image with Glide
        Glide.with(holder.itemView)
            .load(imageUrl)
            .placeholder(R.drawable.ic_placeholder)
            .error(R.drawable.ic_placeholder)
            .into(holder.ivImage)

        // Image click
        holder.ivImage.setOnClickListener {
            if (!item.imageUrl.isNullOrEmpty()) {
                val context = holder.itemView.context
                val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_image_popup, null)

                val ivPopup = dialogView.findViewById<ImageView>(R.id.ivPopupImage)

                val glideUrl = GlideUrl(
                    item.imageUrl,
                    LazyHeaders.Builder()
                        .addHeader("Authorization", "Bearer $jwtToken")
                        .build()
                )

                Glide.with(context)
                    .load(glideUrl)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_placeholder)
                    .into(ivPopup)

                val dialog = android.app.AlertDialog.Builder(context)
                    .setView(dialogView)
                    .setCancelable(true)
                    .create()

                dialog.show()
            } else {
                Toast.makeText(holder.itemView.context, "No photo uploaded", Toast.LENGTH_SHORT).show()
            }
        }

    }

    override fun getItemCount() = items.size
}
