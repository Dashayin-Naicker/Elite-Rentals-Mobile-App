package com.rentals.eliterentals

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

class TenantDashboardActivity : BaseActivity() {

    private val api = RetrofitClient.instance
    private var jwt = ""
    private var tenantId = 0
    private var currentLease: LeaseDto? = null

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
        val leaseCard = findViewById<CardView>(R.id.leaseCard)
        val invoiceCard = findViewById<CardView>(R.id.cardInvoice)
        val leaseEndDaysTv = findViewById<TextView>(R.id.tvLeaseEndDays)

        // Load JWT and tenantId
        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        jwt = prefs.getString("jwt", "") ?: ""
        tenantId = prefs.getInt("userId", 0)
        val tenantName = prefs.getString("tenantName", "Tenant")
        tenantNameTv.text = getString(R.string.greeting, tenantName)

        uploadProofCard.setOnClickListener {
            startActivity(Intent(this, UploadProofActivity::class.java))
        }

        findViewById<CardView>(R.id.cardMaintenance).setOnClickListener {
            currentLease?.let { lease ->
                val intent = Intent(this, ReportMaintenanceActivity::class.java)
                intent.putExtra("leaseId", lease.leaseId)
                intent.putExtra("propertyId", lease.propertyId)
                startActivity(intent)
            } ?: Toast.makeText(this, "No active lease found", Toast.LENGTH_SHORT).show()
        }


        findViewById<LinearLayout>(R.id.navTrackMaintenance).setOnClickListener {
            startActivity(Intent(this, ReportMaintenanceActivity::class.java))
        }

        // Fetch leases safely
        lifecycleScope.launch {
            try {
                val leaseRes = api.getAllLeases("Bearer $jwt")
                val leases = if (leaseRes.isSuccessful) leaseRes.body()?.filter { it.tenantId == tenantId } ?: emptyList() else emptyList()
                val lease = leases.firstOrNull()
                currentLease = lease

                val paymentRes = api.getTenantPayments("Bearer $jwt", tenantId)
                val latestPaymentStatus = if (paymentRes.isSuccessful) {
                    val payments = paymentRes.body() ?: emptyList()
                    payments
                        .mapNotNull { p ->
                            p.date?.let { dateStr ->
                                Pair(LocalDate.parse(dateStr.substring(0, 10)), p.status)
                            }
                        }
                        .maxByOrNull { it.first }
                        ?.second ?: "No Payment"
                } else {
                    "No Payment"
                }

                val latestPaymentDate = if (paymentRes.isSuccessful) {
                    val payments = paymentRes.body() ?: emptyList()
                    payments
                        .mapNotNull { it.date?.substring(0, 10) }
                        .maxOrNull()
                } else null

                if (lease != null) {
                    populateLeaseInfo(
                        leaseInfoTv, rentStatusTv, rentAmountTv,
                        rentDueDaysTv, leaseEndDaysTv, lease, latestPaymentStatus, latestPaymentDate
                    )
                } else {
                    showNoLease(leaseInfoTv, rentStatusTv, rentAmountTv, rentDueDaysTv, leaseEndDaysTv)
                }

                if (lease != null) {
                    populateLeaseInfo(
                        leaseInfoTv, rentStatusTv, rentAmountTv,
                        rentDueDaysTv, leaseEndDaysTv, lease, latestPaymentStatus
                    )
                } else {
                    showNoLease(leaseInfoTv, rentStatusTv, rentAmountTv, rentDueDaysTv, leaseEndDaysTv)
                }
            } catch (e: Exception) {
                Log.e("TenantDashboard", "Failed to load lease or payment", e)
                showError(leaseInfoTv, rentStatusTv, rentAmountTv, rentDueDaysTv, leaseEndDaysTv)
            }
        }

        findViewById<CardView>(R.id.cardTrackMaintenance).setOnClickListener {
            startActivity(Intent(this, TrackMaintenanceActivity::class.java))
        }

        findViewById<ImageView>(R.id.notificationIcon).setOnClickListener {
            startActivity(Intent(this, MessagesActivity::class.java))
        }

        // 🔹 PDF generation actions
        invoiceCard.setOnClickListener {
            currentLease?.let { lease ->
                generateInvoicePdf(lease)
            } ?: Toast.makeText(this, "No active lease found.", Toast.LENGTH_SHORT).show()
        }

        leaseCard.setOnClickListener {
            currentLease?.let { lease ->
                generateLeasePdf(lease)
            } ?: Toast.makeText(this, "No lease details available.", Toast.LENGTH_SHORT).show()
        }
    }

    // Helper to draw logo
    private fun drawLogo(canvas: Canvas, drawableId: Int, centerX: Float, topY: Float, maxWidth: Int): Float {
        val drawable = resources.getDrawable(drawableId, null)
        val aspectRatio = drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight
        val width = maxWidth.toFloat()
        val height = width / aspectRatio

        drawable.setBounds(
            (centerX - width / 2).toInt(),
            topY.toInt(),
            (centerX + width / 2).toInt(),
            (topY + height).toInt()
        )
        drawable.draw(canvas)
        return topY + height + 20f // return new Y after logo + padding
    }

    // 🔹 INVOICE PDF with Logo
    private fun generateInvoicePdf(lease: LeaseDto) {
        val pdf = android.graphics.pdf.PdfDocument()
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        // Draw logo
        var y = drawLogo(canvas, R.drawable.logo, 297f, 20f, 100)

        // Paints
        val titlePaint = Paint().apply {
            textSize = 22f
            isFakeBoldText = true
            color = Color.parseColor("#2E3A59")
            textAlign = Paint.Align.CENTER
        }

        val headerPaint = Paint().apply {
            textSize = 16f
            isFakeBoldText = true
            color = Color.BLACK
        }

        val textPaint = Paint().apply {
            textSize = 14f
            color = Color.DKGRAY
        }

        val tableHeaderPaint = Paint().apply {
            textSize = 14f
            isFakeBoldText = true
            color = Color.WHITE
        }

        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 2f
        }

        val tableBgPaint = Paint().apply {
            color = Color.parseColor("#2E3A59")
        }

        val startX = 60f
        val endX = 535f

        // Title
        y += 10f
        canvas.drawText("ELITE RENTALS", 297f, y, titlePaint)
        y += 30f
        canvas.drawText("MONTHLY INVOICE", 297f, y, titlePaint)
        y += 30f

        // Divider
        canvas.drawLine(startX, y, endX, y, linePaint)
        y += 30f

        val tenantName = getSharedPreferences("app", MODE_PRIVATE).getString("tenantName", "Tenant")
        val rent = lease.property?.rentAmount ?: 0.0

        // Tenant Info
        canvas.drawText("Invoice To:", startX, y, headerPaint)
        y += 20f
        canvas.drawText("Tenant Name: $tenantName", startX, y, textPaint)
        y += 20f
        canvas.drawText("Property: ${lease.property?.title}", startX, y, textPaint)
        y += 20f
        canvas.drawText("Address: ${lease.property?.address}", startX, y, textPaint)
        y += 20f
        canvas.drawText("Invoice Date: ${LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}", startX, y, textPaint)
        y += 40f

        // Table Headers
        val col1 = startX
        val col2 = startX + 300f
        val col3 = endX - 60f

        // Table background
        canvas.drawRect(startX, y, endX, y + 30f, tableBgPaint)
        canvas.drawText("Description", col1 + 5f, y + 20f, tableHeaderPaint)
        canvas.drawText("Quantity", col2 + 5f, y + 20f, tableHeaderPaint)
        canvas.drawText("Amount (R)", col3 + 5f, y + 20f, tableHeaderPaint)

        y += 30f

        // Table rows
        val rowHeight = 25f
        val rentQty = 1
        canvas.drawText("Monthly Rent", col1 + 5f, y + 20f, textPaint)
        canvas.drawText(rentQty.toString(), col2 + 5f, y + 20f, textPaint)
        canvas.drawText(String.format("%.2f", rent), col3 + 5f, y + 20f, textPaint)
        y += rowHeight

        // Add any future items here
        // e.g., utilities, penalties, discounts

        // Total row
        canvas.drawLine(col1, y, endX, y, linePaint)
        y += 10f
        canvas.drawText("Total:", col2 + 5f, y + 20f, headerPaint)
        canvas.drawText(String.format("%.2f", rent), col3 + 5f, y + 20f, headerPaint)
        y += 50f

        // Footer
        canvas.drawLine(startX, y, endX, y, linePaint)
        y += 30f
        canvas.drawText(
            "Thank you for choosing Elite Rentals.",
            297f,
            y,
            Paint().apply {
                textSize = 12f
                color = Color.GRAY
                textAlign = Paint.Align.CENTER
            }
        )

        pdf.finishPage(page)

        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "EliteRentals")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "Invoice_${LocalDate.now()}.pdf")

        pdf.writeTo(FileOutputStream(file))
        pdf.close()

        Toast.makeText(this, "Invoice saved to ${file.path}", Toast.LENGTH_LONG).show()
        openPdf(file)
    }


    // 🔹 LEASE PDF with Logo
    private fun generateLeasePdf(lease: LeaseDto) {
        val pdf = android.graphics.pdf.PdfDocument()
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        // Draw logo
        var y = drawLogo(canvas, R.drawable.logo, 297f, 20f, 100)

        val titlePaint = Paint().apply {
            textSize = 22f
            isFakeBoldText = true
            color = Color.parseColor("#2E3A59")
            textAlign = Paint.Align.CENTER
        }

        val headerPaint = Paint().apply {
            textSize = 16f
            isFakeBoldText = true
            color = Color.BLACK
        }

        val textPaint = Paint().apply {
            textSize = 14f
            color = Color.DKGRAY
        }

        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 2f
        }

        val startX = 60f

        // Title
        y += 10f
        canvas.drawText("ELITE RENTALS", 297f, y, titlePaint)
        y += 30f
        canvas.drawText("LEASE AGREEMENT SUMMARY", 297f, y, titlePaint)
        y += 30f

        canvas.drawLine(startX, y, 535f, y, linePaint)
        y += 40f

        val tenantName = getSharedPreferences("app", MODE_PRIVATE).getString("tenantName", "Tenant")
        val start = lease.startDate.substring(0, 10)
        val end = lease.endDate.substring(0, 10)

        // Tenant Details
        canvas.drawText("Tenant Details", startX, y, headerPaint)
        y += 25f
        canvas.drawText("Name: $tenantName", startX, y, textPaint)
        y += 20f
        canvas.drawText("Property: ${lease.property?.title}", startX, y, textPaint)
        y += 20f
        canvas.drawText("Address: ${lease.property?.address}", startX, y, textPaint)

        // Lease Details
        y += 30f
        canvas.drawLine(startX, y, 535f, y, linePaint)
        y += 30f
        canvas.drawText("Lease Details", startX, y, headerPaint)
        y += 25f
        canvas.drawText("Start Date: $start", startX, y, textPaint)
        y += 20f
        canvas.drawText("End Date: $end", startX, y, textPaint)
        y += 20f
        canvas.drawText("Monthly Rent: R${String.format("%.2f", lease.property?.rentAmount ?: 0.0)}", startX, y, textPaint)

        // Terms
        y += 40f
        canvas.drawLine(startX, y, 535f, y, linePaint)
        y += 30f
        canvas.drawText("Lease Terms", startX, y, headerPaint)
        y += 25f

        val terms = listOf(
            "• Rent must be paid before the last day of each month.",
            "• Tenant is responsible for maintaining the property.",
            "• Elite Rentals reserves the right to inspect the property with notice.",
            "• Lease renewal requires 30 days’ written notice."
        )

        for (term in terms) {
            canvas.drawText(term, startX, y, textPaint)
            y += 20f
        }

        // Footer
        y += 60f
        canvas.drawLine(startX, y, 535f, y, linePaint)
        y += 30f
        canvas.drawText(
            "Authorized by Elite Rentals Management",
            297f,
            y,
            Paint().apply {
                textSize = 12f
                color = Color.GRAY
                textAlign = Paint.Align.CENTER
            }
        )

        pdf.finishPage(page)

        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "EliteRentals")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "Lease_${LocalDate.now()}.pdf")

        pdf.writeTo(FileOutputStream(file))
        pdf.close()

        Toast.makeText(this, "Lease saved to ${file.path}", Toast.LENGTH_LONG).show()
        openPdf(file)
    }



    // 🔹 Open saved PDF
    private fun openPdf(file: File) {
        val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "application/pdf")
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NO_HISTORY
        startActivity(Intent.createChooser(intent, "Open PDF"))
    }

    private fun populateLeaseInfo(
        leaseInfoTv: TextView, rentStatusTv: TextView, rentAmountTv: TextView,
        rentDueDaysTv: TextView, leaseEndDaysTv: TextView,
        lease: LeaseDto, paymentStatus: String,
        latestPaymentDate: String? = null
    ) {
        val startDate = formatDate(lease.startDate)
        val endDate = formatDate(lease.endDate)
        leaseInfoTv.text = getString(R.string.lease_period, startDate, endDate)

        // 🔹 Check if payment is from current month
        val status = if (!latestPaymentDate.isNullOrEmpty()) {
            val paymentMonth = try {
                LocalDate.parse(latestPaymentDate.substring(0, 10)).monthValue
            } catch (e: Exception) { -1 }

            val currentMonth = LocalDate.now().monthValue
            if (paymentMonth != currentMonth) "pending" else paymentStatus
        } else {
            "pending"
        }

        rentStatusTv.text = getString(R.string.rent_status, status.capitalize())

        val statusColor = when (status.lowercase()) {
            "paid" -> Color.parseColor("#4CAF50")
            "pending" -> Color.parseColor("#FFA500")
            "overdue" -> Color.parseColor("#F44336")
            else -> Color.GRAY
        }

        val bg = GradientDrawable().apply {
            cornerRadius = 12f * resources.displayMetrics.density
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
        leaseInfoTv: TextView, rentStatusTv: TextView,
        rentAmountTv: TextView, rentDueDaysTv: TextView, leaseEndDaysTv: TextView
    ) {
        leaseInfoTv.text = getString(R.string.no_active_lease)
        rentStatusTv.text = getString(R.string.rent_status_na)
        rentAmountTv.text = getString(R.string.rent_amount, 0)
        rentDueDaysTv.text = getString(R.string.no_rent_due)
        leaseEndDaysTv.text = getString(R.string.lease_end_na)
    }

    private fun showError(
        leaseInfoTv: TextView, rentStatusTv: TextView,
        rentAmountTv: TextView, rentDueDaysTv: TextView, leaseEndDaysTv: TextView
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
