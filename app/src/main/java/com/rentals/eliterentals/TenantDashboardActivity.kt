package com.rentals.eliterentals

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class TenantDashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tenant_dashboard)

        val cardMaintenance = findViewById<CardView>(R.id.cardMaintenance)

        cardMaintenance.setOnClickListener {
            val intent = Intent(this, ReportMaintenanceActivity::class.java)
            startActivity(intent)
        }
    }
}
