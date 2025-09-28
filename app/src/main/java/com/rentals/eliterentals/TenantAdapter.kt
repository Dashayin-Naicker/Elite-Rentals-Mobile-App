package com.rentals.eliterentals

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import android.widget.Button

class TenantAdapter(
    private var items: MutableList<UserDto>,
    private val onApprove: (UserDto) -> Unit
) : RecyclerView.Adapter<TenantAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name = v.findViewById<TextView>(R.id.tvTenantName)
        val email = v.findViewById<TextView>(R.id.tvTenantEmail)
        val status = v.findViewById<TextView>(R.id.tvTenantStatus)
        val btnApprove = v.findViewById<Button>(R.id.btnApprove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_tenant, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, pos: Int) {
        val u = items[pos]
        holder.name.text = "${u.firstName} ${u.lastName}"
        holder.email.text = u.email
        holder.status.text = "Status: ${u.tenantApproval}"
        holder.btnApprove.isEnabled = u.tenantApproval != "Approved"
        holder.btnApprove.setOnClickListener { onApprove(u) }
    }

    fun submit(newList: List<UserDto>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }
}

