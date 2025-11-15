package com.rentals.eliterentals

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.rentals.eliterentals.AlertAdapter as AAdapter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar
import java.util.Date
import com.github.mikephil.charting.data.Entry
import android.widget.Button
import android.widget.Toast
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream


class AdminDashboardActivity : AppCompatActivity() {

    // KPI views
    private lateinit var kpiTotalLeasesTitle: TextView
    private lateinit var kpiTotalLeasesValue: TextView

    private lateinit var kpiTotalMaintenanceTitle: TextView
    private lateinit var kpiTotalMaintenanceValue: TextView

    private lateinit var kpiTotalPaymentsTitle: TextView
    private lateinit var kpiTotalPaymentsValue: TextView

    private lateinit var kpiOccupancyTitle: TextView
    private lateinit var kpiOccupancyValue: TextView

    private lateinit var rvAlerts: RecyclerView
    private lateinit var rvLeases: RecyclerView
    private lateinit var rvMaintenance: RecyclerView
    private lateinit var rvPayments: RecyclerView

    private lateinit var leaseAdapter: LeaseAdapter
    private lateinit var maintenanceAdapter: AdminMaintenanceAdapter
    private lateinit var paymentAdapter: PaymentAdapter
    private lateinit var alertAdapter: AAdapter

    private lateinit var api: ApiService

    private lateinit var chartPaymentTrends: LineChart
    private lateinit var chartLeaseExpiry: BarChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        findViewById<Button>(R.id.btnExportPdf).setOnClickListener {
            generateDashboardPdf()
        }



        api = RetrofitClient.instance

        // KPI Cards
        val card1 = findViewById<View>(R.id.cardTotalLeases)
        kpiTotalLeasesTitle = card1.findViewById(R.id.kpiTitle)
        kpiTotalLeasesValue = card1.findViewById(R.id.kpiValue)

        val card2 = findViewById<View>(R.id.cardTotalMaintenance)
        kpiTotalMaintenanceTitle = card2.findViewById(R.id.kpiTitle)
        kpiTotalMaintenanceValue = card2.findViewById(R.id.kpiValue)

        val card3 = findViewById<View>(R.id.cardTotalPayments)
        kpiTotalPaymentsTitle = card3.findViewById(R.id.kpiTitle)
        kpiTotalPaymentsValue = card3.findViewById(R.id.kpiValue)

        val card4 = findViewById<View>(R.id.cardOccupancy)
        kpiOccupancyTitle = card4.findViewById(R.id.kpiTitle)
        kpiOccupancyValue = card4.findViewById(R.id.kpiValue)

        kpiTotalLeasesTitle.text = "Total Leases"
        kpiTotalMaintenanceTitle.text = "Open Maintenance"
        kpiTotalPaymentsTitle.text = "Overdue Payments"
        kpiOccupancyTitle.text = "Properties Occupied"

        // RecyclerViews
        rvAlerts = findViewById(R.id.rvAlerts)
        rvLeases = findViewById(R.id.rvLeases)
        rvMaintenance = findViewById(R.id.rvMaintenance)
        rvPayments = findViewById(R.id.rvPayments)

        rvAlerts.layoutManager = LinearLayoutManager(this)
        rvLeases.layoutManager = LinearLayoutManager(this)
        rvMaintenance.layoutManager = LinearLayoutManager(this)
        rvPayments.layoutManager = LinearLayoutManager(this)

        leaseAdapter = LeaseAdapter(emptyList())
        maintenanceAdapter = AdminMaintenanceAdapter(emptyList())
        paymentAdapter = PaymentAdapter(emptyList())
        alertAdapter = AAdapter(emptyList())

        rvLeases.adapter = leaseAdapter
        rvMaintenance.adapter = maintenanceAdapter
        rvPayments.adapter = paymentAdapter
        rvAlerts.adapter = alertAdapter

        // --- Collapsible Item Cards ---
        setupCollapsible(R.id.txtLeaseTitle, R.id.leaseDetails)
        setupCollapsible(R.id.txtMaintTitle, R.id.maintDetails)
        setupCollapsible(R.id.txtPaymentTitle, R.id.paymentDetails)

        // --- Collapsible Section Headers ---
        setupCollapsible(R.id.tvAlertsHeader, R.id.alertsSection)
        setupCollapsible(R.id.tvRecentHeader, R.id.recentSection)
        setupCollapsible(R.id.tvLeasesHeader, R.id.leasesSection)
        setupCollapsible(R.id.tvMaintenanceHeader, R.id.maintenanceSection)
        setupCollapsible(R.id.tvPaymentsHeader, R.id.paymentsSection)

        chartPaymentTrends = findViewById(R.id.chartPaymentTrends)
        chartLeaseExpiry = findViewById(R.id.chartLeaseExpiry)

        loadDashboard()
    }


    fun generateDashboardPdf() {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint().apply {
            textSize = 14f
            color = Color.BLACK
        }

        var y = 40
        fun drawLine(text: String) {
            canvas.drawText(text, 40f, y.toFloat(), paint)
            y += 24
        }

        drawLine("Elite Rentals Admin Dashboard Report")
        drawLine("Date: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}")
        drawLine("")
        drawLine("KPI Summary:")
        drawLine("• Total Leases: ${kpiTotalLeasesValue.text}")
        drawLine("• Open Maintenance: ${kpiTotalMaintenanceValue.text}")
        drawLine("• Overdue Payments: ${kpiTotalPaymentsValue.text}")
        drawLine("• Properties Occupied: ${kpiOccupancyValue.text}")
        drawLine("")
        drawLine("Alerts:")

        for (alert in alertAdapter.alerts) {
            drawLine("• $alert")
        }


        drawLine("")
        drawLine("Charts:")
        drawLine("• Payments Collected: See dashboard for visual trends")
        drawLine("• Expiring Leases: See dashboard for visual trends")

        pdfDocument.finishPage(page)

        val file = File(getExternalFilesDir(null), "dashboard_report.pdf")
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()

        Toast.makeText(this, "PDF report saved to ${file.absolutePath}", Toast.LENGTH_LONG).show()

        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NO_HISTORY
        }

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No PDF viewer found", Toast.LENGTH_SHORT).show()
        }

    }

    // Unified collapsible helper with animation
    private fun setupCollapsible(headerId: Int, sectionId: Int) {
        val header = findViewById<TextView?>(headerId)
        val section = findViewById<View?>(sectionId)

        header?.setOnClickListener {
            section?.let {
                if (it.visibility == View.VISIBLE) {
                    it.animate().alpha(0f).setDuration(200).withEndAction {
                        it.visibility = View.GONE
                        header.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.arrow_down_float, 0)
                        it.alpha = 1f
                    }
                } else {
                    it.visibility = View.VISIBLE
                    it.alpha = 0f
                    it.animate().alpha(1f).setDuration(200).start()
                    header.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.arrow_up_float, 0)
                }
            }
        }
    }

    private fun setupCharts(payments: List<PaymentDto>, leases: List<LeaseDto>) {
        val marker = ChartMarkerView(this, R.layout.marker_view)
        chartPaymentTrends.marker = marker
        chartLeaseExpiry.marker = marker

        val monthFormatter = SimpleDateFormat("MMM", Locale.getDefault())
        val isoParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

        // --- Payments Trends per Month ---
        val paymentsByMonth = payments.groupBy { p ->
            try {
                p.date?.let { isoParser.parse(it) }?.let { monthFormatter.format(it) } ?: "Unknown"
            } catch (e: Exception) {
                "Unknown"
            }
        }

        val paymentEntries = paymentsByMonth.entries.mapIndexed { index, entry ->
            Entry(index.toFloat(), entry.value.sumOf { it.amount ?: 0.0 }.toFloat())
        }

        val paymentDataSet = LineDataSet(paymentEntries, "Payments Collected").apply {
            color = resources.getColor(R.color.brandBlue, theme)
            setDrawFilled(true)
            fillAlpha = 50
            fillColor = resources.getColor(R.color.brandBlue, theme)
            valueTextSize = 12f
        }

        chartPaymentTrends.apply {
            data = LineData(paymentDataSet)
            xAxis.valueFormatter = IndexAxisValueFormatter(paymentsByMonth.keys.toList())
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            axisRight.isEnabled = false
            description.isEnabled = false
            invalidate()
        }

        // --- Lease Expiration Chart ---
        val leaseByMonth = leases.groupBy { l ->
            try {
                l.endDate?.let { isoParser.parse(it) }?.let { monthFormatter.format(it) } ?: "Unknown"
            } catch (e: Exception) {
                "Unknown"
            }
        }

        val leaseEntries = leaseByMonth.entries.mapIndexed { index, entry ->
            BarEntry(index.toFloat(), entry.value.size.toFloat())
        }

        val leaseDataSet = BarDataSet(leaseEntries, "Expiring Leases").apply {
            color = resources.getColor(R.color.badge_warning, theme)
            valueTextSize = 12f
        }

        chartLeaseExpiry.apply {
            data = BarData(leaseDataSet)
            xAxis.valueFormatter = IndexAxisValueFormatter(leaseByMonth.keys.toList())
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            axisRight.isEnabled = false
            description.isEnabled = false
            invalidate()
        }
    }



    private fun loadDashboard() {
        lifecycleScope.launch {
            try {
                val token = "Bearer ${SharedPrefs.getToken(this@AdminDashboardActivity)}"

                val chartLeases = api.getLeases("Bearer ${SharedPrefs.getToken(this@AdminDashboardActivity)}").body() ?: emptyList()
                val chartPayments = api.getPayments("Bearer ${SharedPrefs.getToken(this@AdminDashboardActivity)}").body() ?: emptyList()

                val propertiesResp = api.getAllProperties(token)
                val leasesResp = api.getLeases(token)
                val maintenanceResp = api.getMaintenance(token)
                val paymentsResp = api.getPayments(token)

                val properties = if (propertiesResp.isSuccessful) propertiesResp.body() ?: emptyList() else emptyList()
                val leases = if (leasesResp.isSuccessful) leasesResp.body() ?: emptyList() else emptyList()
                val maint = if (maintenanceResp.isSuccessful) maintenanceResp.body() ?: emptyList() else emptyList()
                val payments = if (paymentsResp.isSuccessful) paymentsResp.body() ?: emptyList() else emptyList()

                // --- Occupancy ---
                val totalProperties = properties.size
                val occupiedProperties = properties.count { it.status?.equals("Occupied", true) == true }
                val occupancyRate = if (totalProperties > 0) (occupiedProperties * 100 / totalProperties) else 0
                kpiOccupancyValue.text = "$occupancyRate%"

                // --- Payments / Overdue ---
                val now = Calendar.getInstance()
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

                val totalPaidThisMonth = payments
                    .filter { it.status?.equals("Paid", true) == true }
                    .filter {
                        val pDate: Date? = it.date?.let { d -> sdf.parse(d) }
                        if (pDate != null) {
                            val pCal = Calendar.getInstance().apply { time = pDate }
                            pCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                                    pCal.get(Calendar.MONTH) == now.get(Calendar.MONTH)
                        } else false
                    }
                    .sumOf { it.amount ?: 0.0 }

                val totalOccupiedRent = properties
                    .filter { it.status?.equals("Occupied", true) == true }
                    .sumOf { it.rentAmount ?: 0.0 }

                val overduePayments = maxOf(totalOccupiedRent - totalPaidThisMonth, 0.0)
                kpiTotalPaymentsValue.text = "R ${String.format("%,.2f", overduePayments)}"

                // --- Other KPIs ---
                kpiTotalLeasesValue.text = leases.size.toString()
                kpiTotalMaintenanceValue.text = maint.count {
                    it.status.equals("Pending", true) || it.status.equals("In Progress", true)
                }.toString()

                // --- Update adapters ---
                leaseAdapter.update(leases)
                maintenanceAdapter.update(maint)
                paymentAdapter.update(payments)

                // --- Alerts ---
                val alerts = mutableListOf<String>()
                payments.filter { !it.status.equals("Paid", true) }
                    .groupBy { it.tenantId }
                    .forEach { (tenantId, list) -> alerts.add("Tenant $tenantId has ${list.size} unpaid payments") }

                properties.filter { !it.status.equals("Occupied", true) }
                    .map { it.title ?: "Property ${it.propertyId}" }
                    .distinct()
                    .forEach { alerts.add("Vacant or not occupied: $it") }

                alertAdapter.update(alerts)

                setupCharts(chartPayments, chartLeases)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
