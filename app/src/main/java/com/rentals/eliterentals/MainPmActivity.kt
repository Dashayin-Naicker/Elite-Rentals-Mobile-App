package com.rentals.eliterentals

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class MainPmActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_pm)

        // --- Load DashboardFragment by default ---
        if (savedInstanceState == null) {
            loadFragment(DashboardFragment())
        }

        // --- Custom Bottom Navigation Clicks ---
        findViewById<View>(R.id.navDashboard).setOnClickListener {
            loadFragment(DashboardFragment())
        }

        findViewById<View>(R.id.navMaintenance).setOnClickListener {
            // Open maintenance ACTIVITY (not a fragment)
            val intent = Intent(this, CaretakerTrackMaintenanceActivity::class.java)
            startActivity(intent)
        }

        findViewById<View>(R.id.navTenants).setOnClickListener {
            loadFragment(TenantsFragment())
        }

        findViewById<View>(R.id.navSettings).setOnClickListener {
            // Open settings ACTIVITY (not a fragment)
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    // --- Helper to Load Fragments into the FrameLayout ---
    fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

}
