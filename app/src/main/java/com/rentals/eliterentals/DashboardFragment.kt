package com.rentals.eliterentals

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment

class DashboardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate your fragment layout (make sure it's named fragment_dashboard.xml)
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        // --- CardView references (instead of Buttons) ---
        val cardManageProperties = view.findViewById<CardView>(R.id.cardManageProperties)
        val cardManageTenants = view.findViewById<CardView>(R.id.cardManageTenants)
        val cardViewLeases = view.findViewById<CardView>(R.id.cardViewLeases)
        val cardAssignCaretaker = view.findViewById<CardView>(R.id.cardAssignCaretaker)

        // --- Navigation handling ---
        cardManageProperties.setOnClickListener {
            (activity as? MainPmActivity)?.loadFragment(PropertiesFragment())
        }

        cardManageTenants.setOnClickListener {
            (activity as? MainPmActivity)?.loadFragment(TenantsFragment())
        }

        cardViewLeases.setOnClickListener {
            (activity as? MainPmActivity)?.loadFragment(LeasesFragment())
        }

        //cardAssignCaretaker.setOnClickListener {
        //    (activity as? MainPmActivity)?.loadFragment(AssignCaretakerFragment())
        //}

        return view
    }
}
