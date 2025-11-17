package com.rentals.eliterentals

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PmPaymentsActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var progress: ProgressBar
    private lateinit var txtEmpty: TextView
    private lateinit var txtError: TextView

    private lateinit var adapter: PmPaymentsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pm_payments)

        // If your theme has an action bar, you can still keep this,
        // but the custom header will handle back navigation visually.
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.title = ""

        // Header back button
        findViewById<ImageView>(R.id.btnBackPayments).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        recycler = findViewById(R.id.recyclerPayments)
        progress = findViewById(R.id.progressBar)
        txtEmpty = findViewById(R.id.txtEmpty)
        txtError = findViewById(R.id.txtError)

        adapter = PmPaymentsAdapter(
            onView = { payment ->
                val intent = PmPaymentDetailActivity.createIntent(this, payment)
                startActivity(intent)
            },
            onConfirm = { payment ->
                changeStatus(payment, "Paid")
            },
            onRevoke = { payment ->
                changeStatus(payment, "Revoked")
            }
        )

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        loadPayments()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun loadPayments() {
        val token = SharedPrefs.getToken(this)
        if (token.isBlank()) {
            Toast.makeText(this, "Missing auth token. Please log in again.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        progress.visibility = View.VISIBLE
        txtError.visibility = View.GONE
        txtEmpty.visibility = View.GONE
        recycler.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.getAllPayments("Bearer $token")
                }

                if (response.isSuccessful) {
                    val payments = response.body().orEmpty()
                    adapter.setItems(payments)

                    if (payments.isEmpty()) {
                        txtEmpty.visibility = View.VISIBLE
                        recycler.visibility = View.GONE
                    } else {
                        txtEmpty.visibility = View.GONE
                        recycler.visibility = View.VISIBLE
                    }
                } else {
                    showError("Failed to load payments (${response.code()})")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showError("Unexpected error loading payments")
            } finally {
                progress.visibility = View.GONE
            }
        }
    }

    private fun changeStatus(payment: PaymentDto, newStatus: String) {
        val token = SharedPrefs.getToken(this)
        if (token.isBlank()) {
            Toast.makeText(this, "Missing auth token. Please log in again.", Toast.LENGTH_LONG).show()
            return
        }

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.updatePaymentStatus(
                        bearer = "Bearer $token",
                        paymentId = payment.paymentId,
                        dto = PaymentStatusDto(status = newStatus)
                    )
                }

                if (response.isSuccessful) {
                    adapter.updateStatus(payment.paymentId, newStatus)
                    val msg = when {
                        newStatus.equals("Paid", ignoreCase = true) ->
                            "Payment confirmed as Paid"
                        newStatus.equals("Revoked", ignoreCase = true) ->
                            "Payment marked as Revoked"
                        else -> "Status updated"
                    }
                    Toast.makeText(this@PmPaymentsActivity, msg, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        this@PmPaymentsActivity,
                        "Failed to update status (${response.code()})",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@PmPaymentsActivity,
                    "Unexpected error updating status",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showError(message: String) {
        txtError.text = message
        txtError.visibility = View.VISIBLE
        recycler.visibility = View.GONE
        txtEmpty.visibility = View.GONE
    }
}
