package com.rentals.eliterentals

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import android.widget.Spinner
import android.widget.EditText
import android.widget.Button
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class LeasesFragment : Fragment() {
    private val api = RetrofitClient.instance

    private var jwt: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_leases, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        jwt = requireContext().getSharedPreferences("app", Context.MODE_PRIVATE)
            .getString("jwt", "") ?: ""

        val spTenant = view.findViewById<Spinner>(R.id.spTenant)
        val spProperty = view.findViewById<Spinner>(R.id.spProperty)
        val etStart = view.findViewById<EditText>(R.id.etStartDate)
        val etEnd = view.findViewById<EditText>(R.id.etEndDate)
        val etDeposit = view.findViewById<EditText>(R.id.etDeposit)
        val btnAssign = view.findViewById<Button>(R.id.btnAssignLease)

        lifecycleScope.launch {
            val usersRes = api.getAllUsers("Bearer $jwt")
            val propsRes = api.getAllProperties("Bearer $jwt") // <- pass bearer here

            if (usersRes.isSuccessful && propsRes.isSuccessful) {
                val tenants = usersRes.body()?.filter { it.role == "Tenant" && it.tenantApproval == "Approved" } ?: emptyList()
                val properties = propsRes.body()?.filter { it.status == "Available" } ?: emptyList()

                spTenant.adapter = ArrayAdapter(
                    requireContext(), android.R.layout.simple_spinner_dropdown_item,
                    tenants.map { "${it.firstName} ${it.lastName}" })
                spTenant.tag = tenants

                spProperty.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item,
                    properties.map { it.address })
                spProperty.tag = properties
            }
        }


        btnAssign.setOnClickListener {
            val tenants = spTenant.tag as List<UserDto>
            val properties = spProperty.tag as List<PropertyDto>

            val selectedTenant = tenants[spTenant.selectedItemPosition]
            val selectedProperty = properties[spProperty.selectedItemPosition]

            val leaseReq = CreateLeaseRequest(
                tenantId = selectedTenant.userId,
                propertyId = selectedProperty.propertyId,
                startDate = etStart.text.toString(),
                endDate = etEnd.text.toString(),
                deposit = etDeposit.text.toString().toDouble()
            )

            lifecycleScope.launch {
                val res = api.createLease("Bearer $jwt", leaseReq)
                if (res.isSuccessful) {
                    Toast.makeText(requireContext(), "Lease Assigned!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Error: ${res.code()}", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }
}
