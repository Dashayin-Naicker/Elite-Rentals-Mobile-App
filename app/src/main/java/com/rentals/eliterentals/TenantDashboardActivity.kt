package com.rentals.eliterentals

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
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
import android.widget.ImageView

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
        tenantNameTv.text = getString(R.string.greeting, tenantName)


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
                // Fetch leases
                val leaseRes = api.getAllLeases("Bearer $jwt")
                val leases = if (leaseRes.isSuccessful) leaseRes.body()?.filter { it.tenantId == tenantId } ?: emptyList() else emptyList()
                val lease = leases.firstOrNull()

                // Fetch tenant payments
                val paymentRes = api.getTenantPayments("Bearer $jwt", tenantId)
                val latestPaymentStatus = if (paymentRes.isSuccessful) {
                    val payments = paymentRes.body() ?: emptyList()
                    payments
                        .mapNotNull { p ->
                            p.date?.let { dateStr ->
                                Pair(LocalDate.parse(dateStr.substring(0, 10)), p.status)
                            }
                        }
                        .maxByOrNull { it.first } // get the latest by date
                        ?.second ?: "No Payment"
                } else {
                    "No Payment"
                }


                if (lease != null) {
                    populateLeaseInfo(
                        leaseInfoTv,
                        rentStatusTv,
                        rentAmountTv,
                        rentDueDaysTv,
                        leaseEndDaysTv,
                        lease,
                        latestPaymentStatus
                    )
                } else {
                    showNoLease(leaseInfoTv, rentStatusTv, rentAmountTv, rentDueDaysTv, leaseEndDaysTv)
                }
            } catch (e: Exception) {
                Log.e("TenantDashboard", "Failed to load lease or payment", e)
                showError(leaseInfoTv, rentStatusTv, rentAmountTv, rentDueDaysTv, leaseEndDaysTv)
            }
        }


        val trackMaintenanceCard = findViewById<CardView>(R.id.cardTrackMaintenance)
        trackMaintenanceCard.setOnClickListener {
            val intent = Intent(this, TrackMaintenanceActivity::class.java)
            startActivity(intent)
        }

        val notificationIcon = findViewById<ImageView>(R.id.notificationIcon)
        notificationIcon.setOnClickListener {
            val intent = Intent(this, MessagesActivity::class.java)
            startActivity(intent)
        }


    }

    private fun populateLeaseInfo(
        leaseInfoTv: TextView,
        rentStatusTv: TextView,
        rentAmountTv: TextView,
        rentDueDaysTv: TextView,
        leaseEndDaysTv: TextView,
        lease: LeaseDto,
        paymentStatus: String,
    ) {
        val startDate = formatDate(lease.startDate)
        val endDate = formatDate(lease.endDate)
        leaseInfoTv.text = getString(R.string.lease_period, startDate, endDate)


        rentStatusTv.text = getString(R.string.rent_status, paymentStatus)


        // Color-code rent status
        val statusColor = when (paymentStatus.lowercase()) {
            "paid" -> Color.parseColor("#4CAF50")    // Green
            "pending" -> Color.parseColor("#FFA500") // Orange
            "overdue" -> Color.parseColor("#F44336") // Red
            else -> Color.GRAY                        // Default
        }

        val bg = GradientDrawable().apply {
            cornerRadius = 12f * resources.displayMetrics.density // maintain dp to px
            setColor(statusColor)
        }

        rentStatusTv.background = bg
        rentStatusTv.setTextColor(Color.WHITE)

        val rent = lease.property?.rentAmount?.toInt() ?: 0
        rentAmountTv.text = getString(R.string.rent_amount, rent)


        val rentDueDays = calculateDaysUntilMonthEnd()
        rentDueDaysTv.text = getString(R.string.rent_due_days, rentDueDays)


        val leaseEndDays = calculateDaysLeft(lease.endDate)
        leaseEndDaysTv.text = getString(R.string.lease_end_days, leaseEndDays)

    }

    private fun showNoLease(
        leaseInfoTv: TextView,
        rentStatusTv: TextView,
        rentAmountTv: TextView,
        rentDueDaysTv: TextView,
        leaseEndDaysTv: TextView
    ) {
        leaseInfoTv.text = getString(R.string.no_active_lease)
        rentStatusTv.text = getString(R.string.rent_status_na)
        rentAmountTv.text = getString(R.string.rent_amount, 0)
        rentDueDaysTv.text = getString(R.string.no_rent_due)
        leaseEndDaysTv.text = getString(R.string.lease_end_na)

    }

    private fun showError(
        leaseInfoTv: TextView,
        rentStatusTv: TextView,
        rentAmountTv: TextView,
        rentDueDaysTv: TextView,
        leaseEndDaysTv: TextView
    ) {
        leaseInfoTv.text = getString(R.string.error_loading_lease)
        rentStatusTv.text = getString(R.string.rent_status_na)
        rentAmountTv.text = getString(R.string.rent_amount, 0)
        rentDueDaysTv.text = getString(R.string.rent_due_days_na)
        leaseEndDaysTv.text = getString(R.string.lease_end_na)

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
