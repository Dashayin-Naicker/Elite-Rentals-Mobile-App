package com.rentals.eliterentals

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SystemUsersAdapter(
    private var items: MutableList<UserDto>,
    private val onApprove: (UserDto) -> Unit,
    private val onToggle: (UserDto) -> Unit
) : RecyclerView.Adapter<SystemUsersAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvTenantName)
        val tvEmail: TextView = view.findViewById(R.id.tvTenantEmail)
        val tvStatus: TextView = view.findViewById(R.id.tvTenantStatus)
        val btnApprove: Button = view.findViewById(R.id.btnApprove)
        val btnToggle: Button = view.findViewById(R.id.btnToggle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_system_user, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val user = items[position]
        holder.tvName.text = "${user.firstName.orEmpty()} ${user.lastName.orEmpty()}".trim()
        holder.tvEmail.text = user.email ?: ""
        val status = when (user.tenantApproval) {
            "Approved" -> "Approved"
            "Pending" -> "Pending"
            null -> if (user.isActive) "Active" else "Disabled"
            else -> user.tenantApproval
        }
        holder.tvStatus.text = "${user.role ?: "User"} • $status"

        // Approve button behavior (enable only if not already approved)
        val approval = user.tenantApproval ?: ""
        holder.btnApprove.isEnabled = approval != "Approved"
        holder.btnApprove.setOnClickListener { onApprove(user) }

        // Toggle enable/disable
        holder.btnToggle.text = if (user.isActive) "Disable" else "Enable"
        holder.btnToggle.setOnClickListener { onToggle(user) }
    }

    fun submit(newList: List<UserDto>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }
}
