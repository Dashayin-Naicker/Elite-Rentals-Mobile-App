package com.rentals.eliterentals

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

class AssignLeaseActivity : AppCompatActivity() {

    private lateinit var tenantSpinner: Spinner
    private lateinit var propSpinner: Spinner
    private lateinit var startEt: EditText
    private lateinit var endEt: EditText
    private lateinit var depositEt: EditText
    private lateinit var btnAssign: Button

    private val api = RetrofitClient.instance
    private var jwt = ""
    private var tenants = listOf<UserDto>()
    private var props = listOf<PropertyDto>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assign_lease)

        tenantSpinner = findViewById(R.id.tenantSpinner)
        propSpinner = findViewById(R.id.propSpinner)
        startEt = findViewById(R.id.startEt)
        endEt = findViewById(R.id.endEt)
        depositEt = findViewById(R.id.depositEt)
        btnAssign = findViewById(R.id.btnAssign)

        // Retrieve JWT
        jwt = getSharedPreferences("app", MODE_PRIVATE)
            .getString("jwt", "") ?: ""

        // Load tenants and properties
        fetchData()

        // Assign lease button
        btnAssign.setOnClickListener {
            assignLease()
        }
    }

    private fun fetchData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                try {
                    val tenantsRes = try { api.getAllUsers("Bearer $jwt") } catch (e: Exception) { null }
                    val propsRes = try { api.getAllProperties("Bearer $jwt") } catch (e: Exception) { null }
                    val leasesRes = try { api.getAllLeases("Bearer $jwt") } catch (e: Exception) { null }

                    val leasedTenantIds = leasesRes?.body()
                        ?.mapNotNull { it.tenantId }
                        ?.toSet() ?: emptySet()

                    // Filter nulls here
                    tenants = tenantsRes?.body()
                        ?.filterNotNull()
                        ?.filter { it.role == "Tenant" && it.tenantApproval == "Approved" && it.userId !in leasedTenantIds }
                        ?: emptyList()

                    tenantSpinner.adapter = ArrayAdapter(
                        this@AssignLeaseActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        tenants.map { "${it.firstName ?: ""} ${it.lastName ?: ""}" }
                    )

                    props = propsRes?.body()?.filterNotNull()?.filter { it.status == "Available" } ?: emptyList()
                    propSpinner.adapter = ArrayAdapter(
                        this@AssignLeaseActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        props.map { it.address ?: it.title ?: "Property ${it.propertyId}" }
                    )

                } catch (e: Exception) {
                    Toast.makeText(this@AssignLeaseActivity, "Error loading data: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }





    private fun assignLease() {
        if (tenantSpinner.selectedItemPosition == -1 || propSpinner.selectedItemPosition == -1) {
            Toast.makeText(this, "Select tenant and property", Toast.LENGTH_SHORT).show()
            return
        }

        val tenantId = tenants[tenantSpinner.selectedItemPosition].userId
        val propId = props[propSpinner.selectedItemPosition].propertyId

        val startDate = startEt.text.toString()
        val endDate = endEt.text.toString()
        val deposit = depositEt.text.toString().toDoubleOrNull()

        if (startDate.isBlank() || endDate.isBlank() || deposit == null) {
            Toast.makeText(this, "Fill all fields correctly", Toast.LENGTH_SHORT).show()
            return
        }

        val leaseRequest = CreateLeaseRequest(
            propertyId = propId,
            tenantId = tenantId,
            startDate = startDate,
            endDate = endDate,
            deposit = deposit
        )

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                try {
                    val response = api.createLease("Bearer $jwt", leaseRequest)
                    if (response.isSuccessful) {
                        Toast.makeText(this@AssignLeaseActivity, "Lease assigned successfully!", Toast.LENGTH_SHORT).show()

                        // Update property status to "Occupied"
                        val statusUpdate = PropertyStatusDto("Occupied")
                        val statusRes = api.updatePropertyStatus("Bearer $jwt", propId, statusUpdate)

                        if (statusRes.isSuccessful) {
                            Toast.makeText(this@AssignLeaseActivity, "Property marked as Occupied", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@AssignLeaseActivity, "Failed to update property status", Toast.LENGTH_SHORT).show()
                        }

                        finish()

                    } else {
                        Toast.makeText(this@AssignLeaseActivity, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@AssignLeaseActivity, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }
}
