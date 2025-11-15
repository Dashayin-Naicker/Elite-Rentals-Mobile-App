package com.rentals.eliterentals

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LeaseAdapter(private var items: List<LeaseDto>) :
    RecyclerView.Adapter<LeaseAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val txtTitle: TextView = v.findViewById(R.id.txtLeaseTitle)
        val txtProp: TextView = v.findViewById(R.id.txtLeaseProp)
        val txtTenant: TextView = v.findViewById(R.id.txtLeaseTenant)
        val txtStatus: TextView = v.findViewById(R.id.txtLeaseStatus)
        val details: View = v.findViewById(R.id.leaseDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_lease, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val l = items[position]
        holder.txtTitle.text = "Lease #${l.leaseId}"
        holder.txtProp.text = l.property?.title ?: "Property ID: ${l.propertyId}"
        holder.txtTenant.text = l.tenant?.firstName?.let { "${it} ${l.tenant?.lastName ?: ""}" } ?: "Tenant ID: ${l.tenantId}"
        holder.txtStatus.text = l.status

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

    fun update(data: List<LeaseDto>) {
        items = data
        notifyDataSetChanged()
    }
}
