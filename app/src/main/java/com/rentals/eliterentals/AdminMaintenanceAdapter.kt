package com.rentals.eliterentals

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AdminMaintenanceAdapter(private var items: List<Maintenance>) :
    RecyclerView.Adapter<AdminMaintenanceAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val txtTitle: TextView = v.findViewById(R.id.txtMaintTitle)
        val txtProperty: TextView = v.findViewById(R.id.txtMaintProperty)
        val txtStatus: TextView = v.findViewById(R.id.txtMaintStatus)
        val details: View = v.findViewById(R.id.maintDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_maintenance, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = items[position]
        holder.txtTitle.text = "Request #${m.maintenanceId}"
        holder.txtProperty.text = m.propertyId.takeIf { it != 0 }?.let { "Property ID: $it" } ?: "Property"
        holder.txtStatus.text = m.status

        // toggle collapse/expand
        holder.txtTitle.setOnClickListener {
            if (holder.details.visibility == View.VISIBLE) {
                holder.details.visibility = View.GONE
                holder.txtTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.arrow_down_float, 0)
            } else {
                holder.details.visibility = View.VISIBLE
                holder.txtTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.arrow_up_float, 0)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun update(data: List<Maintenance>) {
        items = data
        notifyDataSetChanged()
    }
}
