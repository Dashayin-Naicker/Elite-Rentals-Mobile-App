package com.rentals.eliterentals

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class MainPmActivity : AppCompatActivity() {

    // ✅ Enum is defined on the class, not inside Companion
    enum class Tab { DASHBOARD, PROPERTIES, TENANTS }

    companion object {
        const val EXTRA_OPEN_TAB = "open_tab"

        // ✅ Accepts MainPmActivity.Tab? (not Companion.Tab)
        fun createIntent(ctx: Context, tab: Tab? = null): Intent {
            return Intent(ctx, MainPmActivity::class.java).apply {
                tab?.let { putExtra(EXTRA_OPEN_TAB, it.name) }
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_pm)

        // open requested tab (default to DASHBOARD)
        openTabFromIntent(intent)

        // bottom navbar
        findViewById<ImageView>(R.id.navDashboard)?.setOnClickListener {
            openTab(Tab.DASHBOARD)
        }
        findViewById<ImageView>(R.id.navManageProperties)?.setOnClickListener {
            openTab(Tab.PROPERTIES)
        }
        findViewById<ImageView>(R.id.navManageTenants)?.setOnClickListener {
            openTab(Tab.TENANTS)
        }
        findViewById<ImageView>(R.id.navAssignLeases)?.setOnClickListener {
            startActivity(Intent(this, AssignLeaseActivity::class.java))
        }
        findViewById<ImageView>(R.id.navAssignMaintenance)?.setOnClickListener {
            startActivity(Intent(this, PropertyManagerMaintenanceActivity::class.java))
        }
        findViewById<ImageView>(R.id.navRegisterTenant)?.setOnClickListener {
            startActivity(Intent(this, RegisterTenantActivity::class.java))
        }
        findViewById<ImageView>(R.id.navGenerateReport)?.setOnClickListener {
            Toast.makeText(this, "Coming Soon", Toast.LENGTH_SHORT).show()
        }


    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openTabFromIntent(intent)
    }

    private fun openTabFromIntent(intent: Intent) {
        val tab = intent.getStringExtra(EXTRA_OPEN_TAB)
            ?.let { runCatching { Tab.valueOf(it) }.getOrNull() }
            ?: Tab.DASHBOARD
        openTab(tab)
    }
    fun navigateToFragment(fragment: Fragment, addToBackStack: Boolean = false) {
        val tx = supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainer, fragment)

        if (addToBackStack) tx.addToBackStack(fragment::class.java.simpleName)
        tx.commit()
    }
    private fun openTab(tab: Tab) {
        val fragment: Fragment = when (tab) {
            Tab.DASHBOARD  -> DashboardFragment()
            Tab.PROPERTIES -> PropertiesFragment()
            Tab.TENANTS    -> TenantsFragment() // tiny stub below if you don't have it yet
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
