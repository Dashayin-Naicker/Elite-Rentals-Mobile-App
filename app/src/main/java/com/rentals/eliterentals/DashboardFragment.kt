package com.rentals.eliterentals

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class DashboardFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        val btnProperties = view.findViewById<Button>(R.id.btnGoProperties)
        val btnTenants = view.findViewById<Button>(R.id.btnGoTenants)
        val btnLeases = view.findViewById<Button>(R.id.btnGoLeases)

        btnProperties.setOnClickListener {
            (activity as? MainPmActivity)?.loadFragment(PropertiesFragment())
        }

        btnTenants.setOnClickListener {
            (activity as? MainPmActivity)?.loadFragment(TenantsFragment())
        }

        btnLeases.setOnClickListener {
            (activity as? MainPmActivity)?.loadFragment(LeasesFragment())
        }

        return view
    }
}
