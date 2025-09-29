package com.rentals.eliterentals

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class TenantDashboardActivity : AppCompatActivity() {

    private val api = RetrofitClient.instance
    private var jwt = ""
    private var tenantId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tenant_dashboard)

        val leaseInfoTv = findViewById<TextView>(R.id.tvLeaseInfo)
        val rentStatusTv = findViewById<TextView>(R.id.tvRentStatus)
        val rentAmountTv = findViewById<TextView>(R.id.tvRentAmount)
        val tenantNameTv = findViewById<TextView>(R.id.tvTenantName)
        val rentDueDaysTv = findViewById<TextView>(R.id.tvRentDueDays)

        // Load JWT and tenantId
        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        jwt = prefs.getString("jwt", "") ?: ""
        tenantId = prefs.getInt("userId", 0)
        val tenantName = prefs.getString("tenantName", "Tenant")
        tenantNameTv.text = "Hi, $tenantName!"

        // Fetch leases
        lifecycleScope.launch {
            try {
                val res = api.getAllLeases("Bearer $jwt")
                if (res.isSuccessful) {
                    val leases = res.body()?.filter { it.tenantId == tenantId } ?: emptyList()
                    val lease = leases.firstOrNull()

                    if (lease != null) {
                        // Dates
                        val startDate = formatDate(lease.startDate)
                        val endDate = formatDate(lease.endDate)
                        leaseInfoTv.text = "Lease: $startDate → $endDate"

                        // Rent status & amount
                        rentStatusTv.text = "Rent Status: ${lease.status}"
                        val rent = lease.property?.rentAmount?.toInt() ?: 0
                        rentAmountTv.text = "R$rent"

                        // Days left until lease end
                        val daysLeft = calculateDaysLeft(lease.endDate)
                        rentDueDaysTv.text = "Rent Due in $daysLeft Days"

                    } else {
                        leaseInfoTv.text = "No active lease"
                        rentStatusTv.text = "Rent Status: N/A"
                        rentAmountTv.text = "R0"
                        rentDueDaysTv.text = "No rent due"
                    }
                } else {
                    leaseInfoTv.text = "Error loading lease"
                    rentStatusTv.text = "N/A"
                    rentAmountTv.text = "R0"
                    rentDueDaysTv.text = "N/A"
                }
            } catch (e: Exception) {
                Log.e("TenantDashboard", "Failed to load lease", e)
                leaseInfoTv.text = "Error loading lease"
                rentStatusTv.text = "N/A"
                rentAmountTv.text = "R0"
                rentDueDaysTv.text = "N/A"
            }
        }
    }

    private fun calculateDaysLeft(endDate: String): Int {
        return try {
            val leaseEnd = LocalDate.parse(endDate.substring(0, 10))
            val today = LocalDate.now()
            ChronoUnit.DAYS.between(today, leaseEnd).toInt().coerceAtLeast(0)
        } catch (e: Exception) {
            0
        }
    }

    private fun formatDate(dateStr: String): String {
        return try {
            val dt = LocalDate.parse(dateStr.substring(0, 10))
            dt.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
        } catch (e: Exception) {
            dateStr
        }
    }
}
