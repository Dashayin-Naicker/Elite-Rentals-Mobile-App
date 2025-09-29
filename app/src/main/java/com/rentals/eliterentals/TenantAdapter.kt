package com.rentals.eliterentals

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TenantAdapter(
    private var items: MutableList<UserDto>,
    private val onApprove: (UserDto) -> Unit,
    private val onToggle: (UserDto) -> Unit
) : RecyclerView.Adapter<TenantAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvTenantName)
        val tvEmail: TextView = view.findViewById(R.id.tvTenantEmail)
        val tvStatus: TextView = view.findViewById(R.id.tvTenantStatus)
        val btnApprove: Button = view.findViewById(R.id.btnApprove)
        val btnToggle: Button = view.findViewById(R.id.btnToggle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tenant, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, pos: Int) {
        val tenant = items[pos]

        // Display full name safely
        val fullName = listOfNotNull(tenant.firstName, tenant.lastName).joinToString(" ")
        holder.tvName.text = if (fullName.isNotBlank()) fullName else "Unnamed Tenant"

        // Email
        holder.tvEmail.text = tenant.email ?: "No email"

        // Status
        val approval = tenant.tenantApproval ?: "Pending"
        holder.tvStatus.text = "Approval: $approval | Active: ${tenant.isActive}"

        // Approve button (disabled if already approved)
        holder.btnApprove.isEnabled = approval != "Approved"
        holder.btnApprove.setOnClickListener { onApprove(tenant) }

        // Toggle button text
        holder.btnToggle.text = if (tenant.isActive) "Disable" else "Enable"
        holder.btnToggle.setOnClickListener { onToggle(tenant) }
    }

    fun submit(newList: List<UserDto>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }
}
