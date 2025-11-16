package com.rentals.eliterentals

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PropertyManagerMaintenanceAdapter(
    private val items: List<Maintenance>,
    private val caretakers: List<UserDto>,
    private val jwtToken: String,
    private val onAssign: (maintenanceId: Int, caretakerId: Int) -> Unit,
    private val onStatusUpdate: (maintenanceId: Int, status: String) -> Unit
) : RecyclerView.Adapter<PropertyManagerMaintenanceAdapter.MaintenanceViewHolder>() {

    inner class MaintenanceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIssue: TextView = view.findViewById(R.id.tvIssue)
        val tvProperty: TextView = view.findViewById(R.id.tvProperty)
        val tvReportedBy: TextView = view.findViewById(R.id.tvReportedBy)
        val spinnerCaretaker: Spinner = view.findViewById(R.id.spinnerCaretaker)
        val btnAssign: Button = view.findViewById(R.id.btnAssign)
        val spinnerStatus: Spinner = view.findViewById(R.id.spinnerStatus)
        val btnUpdateStatus: Button = view.findViewById(R.id.btnUpdateStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MaintenanceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_assign_maintenance, parent, false) // NEW layout
        return MaintenanceViewHolder(view)
    }


    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: MaintenanceViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context

        holder.tvIssue.text = item.description
        holder.tvProperty.text = "Property #${item.propertyId}"
        holder.tvReportedBy.text = "Tenant #${item.tenantId}"

        // ---------- Caretaker Spinner ----------
        val caretakersOnly = caretakers.filter { it.role?.equals("Caretaker", ignoreCase = true) == true }

        val caretakerNames = if (caretakersOnly.isNotEmpty()) {
            caretakersOnly.map { "${it.firstName ?: ""} ${it.lastName ?: ""}" }
        } else {
            listOf("No caretakers available")
        }

        val adapterCaretaker = ArrayAdapter(context, android.R.layout.simple_spinner_item, caretakerNames)
        adapterCaretaker.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        holder.spinnerCaretaker.adapter = adapterCaretaker

        holder.spinnerCaretaker.tag = caretakersOnly

        item.assignedCaretakerId?.let { assignedId ->
            val index = caretakersOnly.indexOfFirst { it.userId == assignedId }
            if (index >= 0) holder.spinnerCaretaker.setSelection(index)
        }


        // Assign button click
        holder.btnAssign.setOnClickListener {
            val filteredList = holder.spinnerCaretaker.tag as? List<UserDto> ?: emptyList()
            val selectedIndex = holder.spinnerCaretaker.selectedItemPosition
            val selectedCaretaker = filteredList.getOrNull(selectedIndex)
            selectedCaretaker?.let { caretaker ->
                onAssign(item.maintenanceId, caretaker.userId)
            }
        }

        // ---------- Status Spinner ----------
        val statuses = listOf("Pending", "In Progress", "Resolved")
        val statusAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, statuses)
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        holder.spinnerStatus.adapter = statusAdapter
        holder.spinnerStatus.setSelection(statuses.indexOf(item.status))

        holder.btnUpdateStatus.setOnClickListener {
            val selectedStatus = statuses[holder.spinnerStatus.selectedItemPosition]
            onStatusUpdate(item.maintenanceId, selectedStatus)
        }
    }


}
