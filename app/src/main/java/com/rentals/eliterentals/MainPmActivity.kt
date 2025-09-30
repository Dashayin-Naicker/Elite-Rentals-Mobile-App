package com.rentals.eliterentals

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainPmActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_pm)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        // Load default
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, DashboardFragment())
                .commit()
        }

        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_dashboard -> {
                    loadFragment(DashboardFragment())
                    true
                }
                R.id.nav_properties -> {
                    loadFragment(PropertiesFragment())
                    true
                }
                R.id.nav_tenants -> {
                    loadFragment(TenantsFragment())
                    true
                }
                R.id.nav_add_tenants -> {
                    val intent = Intent(this, RegisterTenantActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_leases -> {
                    loadFragment(LeasesFragment())
                    true
                }
                else -> false
            }
        }
    }

    fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }

}
