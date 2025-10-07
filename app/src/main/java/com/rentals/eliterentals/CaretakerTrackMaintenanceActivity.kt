package com.rentals.eliterentals

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class CaretakerTrackMaintenanceActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_caretaker_track_maintenance)

        // Back button functionality
        findViewById<ImageView>(R.id.ic_back).setOnClickListener {
            finish()
        }

        // Bottom Navigation Clicks
        findViewById<ImageView>(R.id.navManageProperties).setOnClickListener {
            navigateToActivity(PropertiesFragment::class.java)
        }

        findViewById<ImageView>(R.id.navManageTenants).setOnClickListener {
            navigateToActivity(TenantsFragment::class.java)
        }

        findViewById<ImageView>(R.id.navAssignLeases).setOnClickListener {
            navigateToActivity(AssignLeaseActivity::class.java)
        }

        findViewById<ImageView>(R.id.navAssignMaintenance).setOnClickListener {
            // Already on this screen
        }

        findViewById<ImageView>(R.id.navRegisterTenant).setOnClickListener {
            navigateToActivity(RegisterTenantActivity::class.java)
        }

        findViewById<ImageView>(R.id.navGenerateReport).setOnClickListener {
            // Placeholder for future feature
        }
    }

    private fun navigateToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
        finish() // Optional: close current activity to prevent back stack clutter
    }
}
