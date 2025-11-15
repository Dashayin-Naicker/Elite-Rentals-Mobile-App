package com.rentals.eliterentals

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PaymentAdapter(private var items: List<PaymentDto>) :
    RecyclerView.Adapter<PaymentAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val txtTitle: TextView = v.findViewById(R.id.txtPaymentTitle)
        val txtTenant: TextView = v.findViewById(R.id.txtPaymentTenant)
        val txtAmount: TextView = v.findViewById(R.id.txtPaymentAmount)
        val txtStatus: TextView = v.findViewById(R.id.txtPaymentStatus)
        val details: View = v.findViewById(R.id.paymentDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_payment, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = items[position]
        holder.txtTitle.text = "Payment #${p.paymentId}"
        holder.txtTenant.text = "Tenant ID: ${p.tenantId}"
        holder.txtAmount.text = "R ${String.format("%,.2f", p.amount)}"
        holder.txtStatus.text = p.status

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

    fun update(data: List<PaymentDto>) {
        items = data
        notifyDataSetChanged()
    }
}
