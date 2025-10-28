package com.rentals.eliterentals

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CaretakerTrackMaintenanceActivity : AppCompatActivity() {
// adding a comment cause someone meeeesssed the repo up teehee
    // addding more
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_caretaker_track_maintenance)

        // 🔙 Back -> Manager Dashboard
        findViewById<ImageView>(R.id.ic_back)?.setOnClickListener {
            goToManagerDashboard()
        }

        // ✅ Bottom navbar
        findViewById<ImageView>(R.id.navDashboard).setOnClickListener {
            startActivity(MainPmActivity.createIntent(this, MainPmActivity.Tab.DASHBOARD))
            finish()
        }
        findViewById<ImageView>(R.id.navManageProperties).setOnClickListener {
            startActivity(MainPmActivity.createIntent(this, MainPmActivity.Tab.PROPERTIES))
            finish()
        }
        findViewById<ImageView>(R.id.navManageTenants).setOnClickListener {
            startActivity(MainPmActivity.createIntent(this, MainPmActivity.Tab.TENANTS))
            finish()
        }
        findViewById<ImageView>(R.id.navAssignLeases).setOnClickListener {
            startActivity(Intent(this, AssignLeaseActivity::class.java))
        }
        findViewById<ImageView>(R.id.navAssignMaintenance).setOnClickListener {
            Toast.makeText(this, "Already on Assign Maintenance", Toast.LENGTH_SHORT).show()
        }
        findViewById<ImageView>(R.id.navRegisterTenant).setOnClickListener {
            startActivity(Intent(this, RegisterTenantActivity::class.java))
        }
        findViewById<ImageView>(R.id.navGenerateReport).setOnClickListener {
            Toast.makeText(this, "Coming Soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun goToManagerDashboard() {
        startActivity(
            MainPmActivity.createIntent(this, MainPmActivity.Tab.DASHBOARD)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        finish()
    }
}
