package com.rentals.eliterentals

import android.app.AlertDialog
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

class CaretakerMaintenanceAdapter(
    private val items: List<Maintenance>,
    private val jwtToken: String,
    private val onStatusChange: (Maintenance, String) -> Unit,
    private val onEditPhoto: (Maintenance) -> Unit
) : RecyclerView.Adapter<CaretakerMaintenanceAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvTitle)
        val status: TextView = view.findViewById(R.id.tvStatus)
        val loggedTime: TextView = view.findViewById(R.id.tvDate)
        val ivImage: ImageView = view.findViewById(R.id.ivImage)
        val ivEditPhoto: ImageView = view.findViewById(R.id.ivEditPhoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_maintenance, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context

        holder.title.text = item.description
        holder.loggedTime.text = context.getString(R.string.logged_at, item.createdAt)

        // ✅ Apply color and text styling for status
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
            else -> {
                holder.status.setBackgroundResource(R.drawable.bg_status_default)
                item.status
            }
        }
        holder.status.text = statusText

        // ✅ Load image securely with JWT header
        val imageUrl = item.imageUrl?.let {
            GlideUrl(
                it, LazyHeaders.Builder()
                    .addHeader("Authorization", "Bearer $jwtToken")
                    .build()
            )
        }
        Glide.with(context)
            .load(imageUrl)
            .placeholder(R.drawable.ic_placeholder)
            .error(R.drawable.ic_placeholder)
            .into(holder.ivImage)

        // ✅ Fullscreen image view popup
        holder.ivImage.setOnClickListener {
            if (!item.imageUrl.isNullOrEmpty()) {
                val dialogView = LayoutInflater.from(context)
                    .inflate(R.layout.dialog_image_popup, null)
                val ivPopup = dialogView.findViewById<ImageView>(R.id.ivPopupImage)

                Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_placeholder)
                    .into(ivPopup)

                AlertDialog.Builder(context)
                    .setView(dialogView)
                    .show()
            } else {
                Toast.makeText(context, "No photo uploaded.", Toast.LENGTH_SHORT).show()
            }
        }

        // ✅ Allow caretaker to change status
        holder.status.setOnClickListener {
            val options = arrayOf("Pending", "In Progress", "Resolved")
            AlertDialog.Builder(context)
                .setTitle("Update Status")
                .setItems(options) { _, which ->
                    val newStatus = options[which]

                    // 1. Update the local item
                    item.status = newStatus

                    // 2. Notify adapter to rebind this item (will apply new color)
                    notifyItemChanged(position)

                    // 3. Call your backend/update callback
                    onStatusChange(item, newStatus)
                }
                .show()
        }


        // ✅ Allow caretaker to update photo
        holder.ivEditPhoto.setOnClickListener {
            onEditPhoto(item)
        }
    }

    override fun getItemCount() = items.size
}