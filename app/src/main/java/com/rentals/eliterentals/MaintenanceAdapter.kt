package com.rentals.eliterentals

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders

class MaintenanceAdapter(
    private val items: List<Maintenance>,
    private val jwtToken: String
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
        val context = holder.itemView.context

        holder.title.text = item.description
        holder.loggedTime.text = context.getString(R.string.logged_at, item.createdAt)

        // Localize and style status
        val statusText = when (item.status) {
            "Pending" -> {
                holder.status.setBackgroundResource(R.drawable.bg_status_pending)
                context.getString(R.string.status_pending)
            }
            "In Progress" -> {
                holder.status.setBackgroundResource(R.drawable.bg_status_in_progress)
                context.getString(R.string.status_in_progress)
            }
            "Resolved" -> {
                holder.status.setBackgroundResource(R.drawable.bg_status_resolved)
                context.getString(R.string.status_resolved)
            }
            else -> item.status
        }
        holder.status.text = statusText

        // Load image with auth header
        val imageUrl = item.imageUrl?.let {
            GlideUrl(it, LazyHeaders.Builder()
                .addHeader("Authorization", "Bearer $jwtToken")
                .build())
        }

        Glide.with(context)
            .load(imageUrl)
            .placeholder(R.drawable.ic_placeholder)
            .error(R.drawable.ic_placeholder)
            .into(holder.ivImage)

        // Image click popup
        holder.ivImage.setOnClickListener {
            if (!item.imageUrl.isNullOrEmpty()) {
                val dialogView = LayoutInflater.from(context)
                    .inflate(R.layout.dialog_image_popup, null)
                val ivPopup = dialogView.findViewById<ImageView>(R.id.ivPopupImage)

                val glideUrl = GlideUrl(item.imageUrl, LazyHeaders.Builder()
                    .addHeader("Authorization", "Bearer $jwtToken")
                    .build())

                Glide.with(context)
                    .load(glideUrl)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_placeholder)
                    .into(ivPopup)

                AlertDialog.Builder(context)
                    .setView(dialogView)
                    .setCancelable(true)
                    .create()
                    .show()
            } else {
                Toast.makeText(context, context.getString(R.string.no_photo_uploaded), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemCount() = items.size
}
