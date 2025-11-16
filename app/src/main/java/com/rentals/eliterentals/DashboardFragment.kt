package com.rentals.eliterentals

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import android.widget.ImageView

class DashboardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        val cardManageProperties = view.findViewById<CardView>(R.id.cardManageProperties)
        val cardManageTenants = view.findViewById<CardView>(R.id.cardManageTenants)
        val cardViewLeases = view.findViewById<CardView>(R.id.cardViewLeases)
        val cardAssignMaintenance = view.findViewById<CardView>(R.id.cardAssignCaretaker)
        val cardRegisterTenant = view.findViewById<CardView>(R.id.cardRegisterTenant)
        val cardGenerateReport = view.findViewById<CardView>(R.id.cardReports)
        val cardSettings = view.findViewById<CardView>(R.id.cardSettings)

        // ✅ Switch tabs inside MainPmActivity (host) instead of trying to start a Fragment as an Activity
        cardManageProperties.setOnClickListener {
            startActivity(MainPmActivity.createIntent(requireContext(), MainPmActivity.Tab.PROPERTIES))
        }

        cardManageTenants.setOnClickListener {
            startActivity(MainPmActivity.createIntent(requireContext(), MainPmActivity.Tab.TENANTS))
        }

        // Real activities:
        cardViewLeases.setOnClickListener {
            startActivity(Intent(requireContext(), AssignLeaseActivity::class.java))
        }

        cardAssignMaintenance.setOnClickListener {
            startActivity(Intent(requireContext(), PropertyManagerMaintenanceActivity::class.java))
        }

        cardRegisterTenant.setOnClickListener {
            startActivity(Intent(requireContext(), RegisterTenantActivity::class.java))
        }

        cardSettings.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }

        cardGenerateReport.setOnClickListener {
            Toast.makeText(requireContext(), "Coming Soon", Toast.LENGTH_SHORT).show()
        }

        val notificationIcon = view.findViewById<ImageView>(R.id.notificationIcon)
        notificationIcon.setOnClickListener {
            startActivity(Intent(requireContext(), MessagesActivity::class.java))
        }

        return view
    }
}
