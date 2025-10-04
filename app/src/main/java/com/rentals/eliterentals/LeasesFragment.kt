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
            try {
                val usersRes = try { api.getAllUsers("Bearer $jwt") } catch (e: Exception) { null }
                val propsRes = try { api.getAllProperties("Bearer $jwt") } catch (e: Exception) { null }
                val leasesRes = try { api.getAllLeases("Bearer $jwt") } catch (e: Exception) { null }

                val leasedTenantIds = leasesRes?.body()
                    ?.mapNotNull { it.tenantId }
                    ?.toSet() ?: emptySet()

                // Filter out nulls here
                val tenants = usersRes?.body()
                    ?.filterNotNull()
                    ?.filter { it.role == "Tenant" && it.tenantApproval == "Approved" && it.userId !in leasedTenantIds }
                    ?: emptyList()

                val properties = propsRes?.body()
                    ?.filterNotNull()
                    ?.filter { it.status == "Available" }
                    ?: emptyList()

                spTenant.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    tenants.map { "${it.firstName ?: ""} ${it.lastName ?: ""}" } // safe access
                )
                spTenant.tag = tenants

                spProperty.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    properties.map { it.address ?: it.title ?: "Property ${it.propertyId}" } // safe access
                )
                spProperty.tag = properties

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading data: ${e.message}", Toast.LENGTH_LONG).show()
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
                try {
                    val res = api.createLease("Bearer $jwt", leaseReq)
                    if (res.isSuccessful) {
                        Toast.makeText(requireContext(), "Lease Assigned!", Toast.LENGTH_SHORT).show()

                        val statusUpdate = PropertyStatusDto("Occupied")
                        val statusRes = api.updatePropertyStatus("Bearer $jwt", selectedProperty.propertyId, statusUpdate)

                        if (statusRes.isSuccessful) {
                            Toast.makeText(requireContext(), "Property marked as Occupied", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "Failed to update property status", Toast.LENGTH_SHORT).show()
                        }

                    } else {
                        Toast.makeText(requireContext(), "Error: ${res.code()}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

}
