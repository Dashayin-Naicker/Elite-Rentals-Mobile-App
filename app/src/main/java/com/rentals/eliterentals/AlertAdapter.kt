package com.rentals.eliterentals

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AlertAdapter(private var items: List<String>) :
    RecyclerView.Adapter<AlertAdapter.VH>() {

    val alerts: List<String>
        get() = items

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val badge: View = v.findViewById(R.id.alertBadge)
        val txtMessage: TextView = v.findViewById(R.id.txtAlertMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_alert, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val msg = items[position]
        holder.txtMessage.text = msg
        holder.badge.setBackgroundColor(
            when {
                msg.contains("expired", true) || msg.contains("vacant", true) ->
                    holder.itemView.context.getColor(R.color.badge_danger)
                msg.contains("overdue", true) ->
                    holder.itemView.context.getColor(R.color.badge_warning)
                else ->
                    holder.itemView.context.getColor(R.color.badge_info)
            }
        )
    }

    override fun getItemCount(): Int = items.size

    fun update(data: List<String>) {
        items = data
        notifyDataSetChanged()
    }
}
