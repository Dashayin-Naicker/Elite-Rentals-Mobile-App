package com.rentals.eliterentals

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rentals.eliterentals.R

class SelectedImageAdapter(
    private val imageList: MutableList<String>,   // image paths / URLs
    private val onRemove: (String) -> Unit        // callback for delete
) : RecyclerView.Adapter<SelectedImageAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val img: ImageView = itemView.findViewById(R.id.imgSelected)
        val btnRemove: TextView = itemView.findViewById(R.id.btnRemove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_selected_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun getItemCount(): Int = imageList.size

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imagePath = imageList[position]

        // Load the image (Coil/Glide compatible)
        Glide.with(holder.itemView)
            .load(imagePath)
            .centerCrop()
            .into(holder.img)

        // Show delete badge only if callback is enabled
        holder.btnRemove.visibility = View.VISIBLE

        holder.btnRemove.setOnClickListener {
            val removed = imageList[position]
            onRemove(removed)

            imageList.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, imageList.size)
        }
    }
}
