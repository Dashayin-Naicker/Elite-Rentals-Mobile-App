package com.rentals.eliterentals

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class PmPaymentsAdapter(
    private val onView: (PaymentDto) -> Unit,
    private val onConfirm: (PaymentDto) -> Unit,
    private val onRevoke: (PaymentDto) -> Unit
) : RecyclerView.Adapter<PmPaymentsAdapter.PaymentViewHolder>() {

    private val items = mutableListOf<PaymentDto>()

    fun setItems(newItems: List<PaymentDto>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun updateStatus(paymentId: Int, newStatus: String) {
        val index = items.indexOfFirst { it.paymentId == paymentId }
        if (index != -1) {
            val old = items[index]
            items[index] = old.copy(status = newStatus)
            notifyItemChanged(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_pm_payment, parent, false)
        return PaymentViewHolder(view)
    }

    override fun onBindViewHolder(holder: PaymentViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class PaymentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val cardRoot: CardView = itemView.findViewById(R.id.cardRoot)
        private val txtTenant: TextView = itemView.findViewById(R.id.txtTenant)
        private val txtAmount: TextView = itemView.findViewById(R.id.txtAmount)
        private val txtStatus: TextView = itemView.findViewById(R.id.txtStatus)
        private val txtDate: TextView = itemView.findViewById(R.id.txtDate)

        private val btnView: Button = itemView.findViewById(R.id.btnView)
        private val btnConfirm: Button = itemView.findViewById(R.id.btnConfirm)
        private val btnRevoke: Button = itemView.findViewById(R.id.btnRevoke)

        fun bind(payment: PaymentDto) {
            txtTenant.text = "Tenant #${payment.tenantId}"
            txtAmount.text = "R %.2f".format(payment.amount)
            txtDate.text = payment.date ?: ""

            val statusLower = payment.status.lowercase()

            when {
                statusLower == "paid" -> {
                    txtStatus.text = "Paid"
                    txtStatus.setBackgroundColor(Color.parseColor("#4CAF50"))
                    txtStatus.setTextColor(Color.WHITE)

                    btnConfirm.isEnabled = false
                    btnRevoke.isEnabled = true
                }
                statusLower == "revoked" -> {
                    txtStatus.text = "Revoked"
                    txtStatus.setBackgroundColor(Color.parseColor("#F44336"))
                    txtStatus.setTextColor(Color.WHITE)

                    btnConfirm.isEnabled = true    // allow manager to re-confirm if needed
                    btnRevoke.isEnabled = false
                }
                else -> { // Pending / anything else
                    txtStatus.text = "Pending"
                    txtStatus.setBackgroundColor(Color.parseColor("#FFC107"))
                    txtStatus.setTextColor(Color.BLACK)

                    btnConfirm.isEnabled = true
                    btnRevoke.isEnabled = true
                }
            }

            btnView.setOnClickListener { onView(payment) }
            btnConfirm.setOnClickListener { onConfirm(payment) }
            btnRevoke.setOnClickListener { onRevoke(payment) }

            cardRoot.setOnClickListener { onView(payment) }
        }
    }
}
