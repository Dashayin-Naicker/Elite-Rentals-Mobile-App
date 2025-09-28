package com.rentals.eliterentals

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class PropertyAdapter(
    private var list: MutableList<PropertyDto>,
    private val onDelete: (PropertyDto) -> Unit,
    private val onEdit: (PropertyDto) -> Unit
) : RecyclerView.Adapter<PropertyAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvAddress: TextView = view.findViewById(R.id.tvAddress)
        val tvRent: TextView = view.findViewById(R.id.tvRent)
        val ivImage: ImageView = view.findViewById(R.id.ivProperty)
        val btnDelete: Button = view.findViewById(R.id.btnDelete)
        val btnEdit: Button = view.findViewById(R.id.btnEdit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_property, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = list[position]
        holder.tvTitle.text = p.title ?: "No title"
        holder.tvAddress.text = p.address ?: "No address"
        holder.tvRent.text = "R${p.rentAmount ?: 0.0}"

        Glide.with(holder.itemView)
            .load(p.imageUrl) // <-- use property, not function
            .placeholder(R.drawable.ic_placeholder)
            .error(R.drawable.ic_placeholder)
            .into(holder.ivImage)

        holder.btnDelete.setOnClickListener { onDelete(p) }
        holder.btnEdit.setOnClickListener { onEdit(p) }
    }

    override fun getItemCount(): Int = list.size

    fun submit(newList: List<PropertyDto>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }
}
