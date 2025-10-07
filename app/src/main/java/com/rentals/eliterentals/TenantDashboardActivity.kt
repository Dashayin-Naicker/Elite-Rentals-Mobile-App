package com.rentals.eliterentals

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import android.widget.LinearLayout
import com.rentals.eliterentals.SettingsActivity

class TenantDashboardActivity : BaseActivity() {

    private val api = RetrofitClient.instance
    private var jwt = ""
    private var tenantId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(ThemeUtils.getSavedTheme(this))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tenant_dashboard)

        val settingsNav = findViewById<LinearLayout>(R.id.navSettings)
        settingsNav.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }


        // UI elements
        val uploadProofCard = findViewById<CardView>(R.id.cardUploadProof)
        val leaseInfoTv = findViewById<TextView>(R.id.tvLeaseInfo)
        val rentStatusTv = findViewById<TextView>(R.id.tvRentStatus)
        val rentAmountTv = findViewById<TextView>(R.id.tvRentAmount)
        val tenantNameTv = findViewById<TextView>(R.id.tvTenantName)
        val rentDueDaysTv = findViewById<TextView>(R.id.tvRentDueDays)
        val leaseEndDaysTv = findViewById<TextView>(R.id.tvLeaseEndDays)

        // Load JWT and tenantId
        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        jwt = prefs.getString("jwt", "") ?: ""
        tenantId = prefs.getInt("userId", 0)
        val tenantName = prefs.getString("tenantName", "Tenant")
        tenantNameTv.text = "Hi, $tenantName!"

        // Navigate to UploadProofActivity
        uploadProofCard.setOnClickListener {
            val intent = Intent(this, UploadProofActivity::class.java)
            startActivity(intent)
        }
        val maintenanceCard = findViewById<CardView>(R.id.cardMaintenance)

        maintenanceCard.setOnClickListener {
            val intent = Intent(this, ReportMaintenanceActivity::class.java)
            startActivity(intent)
        }

        findViewById<LinearLayout>(R.id.navTrackMaintenance).setOnClickListener {
            val intent = Intent(this, ReportMaintenanceActivity::class.java)
            startActivity(intent)
        }

        // Fetch leases safely
        lifecycleScope.launch {
            try {
                val res = api.getAllLeases("Bearer $jwt")
                if (res.isSuccessful) {
                    val leases = res.body()?.filter { it.tenantId == tenantId } ?: emptyList()
                    val lease = leases.firstOrNull()

                    if (lease != null) {
                        prefs.edit().putInt("propertyId", lease.property?.propertyId ?: -1).apply()

                        populateLeaseInfo(
                            leaseInfoTv, rentStatusTv, rentAmountTv,
                            rentDueDaysTv, leaseEndDaysTv, lease
                        )
                    } else {
                        showNoLease(leaseInfoTv, rentStatusTv, rentAmountTv, rentDueDaysTv, leaseEndDaysTv)
                    }
                } else {
                    val rawError = res.errorBody()?.string()
                    Log.e("TenantDashboard", "API Error: ${res.code()} $rawError")
                    showError(leaseInfoTv, rentStatusTv, rentAmountTv, rentDueDaysTv, leaseEndDaysTv)
                }
            } catch (e: java.io.EOFException) {
                Log.e("TenantDashboard", "EOFException: API response empty", e)
                showError(leaseInfoTv, rentStatusTv, rentAmountTv, rentDueDaysTv, leaseEndDaysTv)
            } catch (e: Exception) {
                Log.e("TenantDashboard", "Failed to load lease", e)
                showError(leaseInfoTv, rentStatusTv, rentAmountTv, rentDueDaysTv, leaseEndDaysTv)
            }
        }
        val trackMaintenanceCard = findViewById<CardView>(R.id.cardTrackMaintenance)
        trackMaintenanceCard.setOnClickListener {
            val intent = Intent(this, TrackMaintenanceActivity::class.java)
            startActivity(intent)
        }

    }

    private fun populateLeaseInfo(
        leaseInfoTv: TextView,
        rentStatusTv: TextView,
        rentAmountTv: TextView,
        rentDueDaysTv: TextView,
        leaseEndDaysTv: TextView,
        lease: LeaseDto
    ) {
        val startDate = formatDate(lease.startDate)
        val endDate = formatDate(lease.endDate)
        leaseInfoTv.text = "Lease: $startDate → $endDate"

        rentStatusTv.text = "Rent Status: ${lease.status}"
        val rent = lease.property?.rentAmount?.toInt() ?: 0
        rentAmountTv.text = "R$rent"

        val rentDueDays = calculateDaysUntilMonthEnd()
        rentDueDaysTv.text = "Rent Due in $rentDueDays Days"

        val leaseEndDays = calculateDaysLeft(lease.endDate)
        leaseEndDaysTv.text = "Lease Ends in $leaseEndDays Days"
    }

    private fun showNoLease(
        leaseInfoTv: TextView,
        rentStatusTv: TextView,
        rentAmountTv: TextView,
        rentDueDaysTv: TextView,
        leaseEndDaysTv: TextView
    ) {
        leaseInfoTv.text = "No active lease"
        rentStatusTv.text = "Rent Status: N/A"
        rentAmountTv.text = "R0"
        rentDueDaysTv.text = "No rent due"
        leaseEndDaysTv.text = "Lease Ends: N/A"
    }

    private fun showError(
        leaseInfoTv: TextView,
        rentStatusTv: TextView,
        rentAmountTv: TextView,
        rentDueDaysTv: TextView,
        leaseEndDaysTv: TextView
    ) {
        leaseInfoTv.text = "Error loading lease"
        rentStatusTv.text = "N/A"
        rentAmountTv.text = "R0"
        rentDueDaysTv.text = "N/A"
        leaseEndDaysTv.text = "N/A"
    }

    private fun calculateDaysUntilMonthEnd(): Int {
        val today = LocalDate.now()
        val lastDay = today.withDayOfMonth(today.lengthOfMonth())
        return ChronoUnit.DAYS.between(today, lastDay).toInt().coerceAtLeast(0)
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
