package com.rentals.eliterentals

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class MainPmActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_pm)

        // Set default fragment
        if (savedInstanceState == null) {
            navigateToFragment(DashboardFragment())
        }

        // Bottom navigation clicks
        findViewById<ImageView>(R.id.navManageProperties).setOnClickListener {
            navigateToFragment(PropertiesFragment())
        }

        findViewById<ImageView>(R.id.navManageTenants).setOnClickListener {
            navigateToFragment(TenantsFragment())
        }

        findViewById<ImageView>(R.id.navAssignLeases).setOnClickListener {
            navigateToActivity(AssignLeaseActivity::class.java)
        }

        findViewById<ImageView>(R.id.navAssignMaintenance).setOnClickListener {
            navigateToActivity(CaretakerTrackMaintenanceActivity::class.java)
        }

        findViewById<ImageView>(R.id.navRegisterTenant).setOnClickListener {
            navigateToActivity(RegisterTenantActivity::class.java)
        }

        findViewById<ImageView>(R.id.navGenerateReport).setOnClickListener {
            Toast.makeText(this, "Coming Soon", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to navigate to a Fragment
    fun navigateToFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    // Function to navigate to an Activity
    private fun navigateToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
    }
}
