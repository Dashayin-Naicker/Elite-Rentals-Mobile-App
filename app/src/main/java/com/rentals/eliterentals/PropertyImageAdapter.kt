package com.rentals.eliterentals

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class PropertyImageAdapter(private val images: List<String>) :
    RecyclerView.Adapter<PropertyImageAdapter.ImageVH>() {

    inner class ImageVH(view: View) : RecyclerView.ViewHolder(view) {
        val iv: ImageView = view.findViewById(R.id.ivPropImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_property_image, parent, false)
        return ImageVH(view)
    }

    override fun onBindViewHolder(holder: ImageVH, position: Int) {
        Glide.with(holder.itemView)
            .load(images[position])
            .placeholder(R.drawable.ic_placeholder)
            .error(R.drawable.ic_placeholder)
            .into(holder.iv)
    }

    override fun getItemCount(): Int = images.size
}
