package com.rentals.eliterentals

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MaintenanceAdapter(private val items: List<Maintenance>) :
    RecyclerView.Adapter<MaintenanceAdapter.MaintenanceViewHolder>() {

    inner class MaintenanceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvTitle)
        val status: TextView = view.findViewById(R.id.tvStatus)
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

        // Color-code status
        when (item.status) {
            "Pending" -> holder.status.setBackgroundResource(R.drawable.bg_status_pending)
            "In Progress" -> holder.status.setBackgroundResource(R.drawable.bg_status_in_progress)
            "Resolved" -> holder.status.setBackgroundResource(R.drawable.bg_status_resolved)
        }
    }

    override fun getItemCount() = items.size
}
