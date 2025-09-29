package com.rentals.eliterentals

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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
            try {
                val tenantsRes = api.getAllUsers("Bearer $jwt")
                val propsRes = api.getAllProperties("Bearer $jwt")

                if (tenantsRes.isSuccessful) {
                    tenants = tenantsRes.body()?.filter { it.role == "Tenant" && it.tenantApproval == "Approved" } ?: emptyList()
                    tenantSpinner.adapter = ArrayAdapter(
                        this@AssignLeaseActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        tenants.map { "${it.firstName} ${it.lastName}" }
                    )
                } else {
                    Toast.makeText(this@AssignLeaseActivity, "Failed to load tenants", Toast.LENGTH_SHORT).show()
                }

                if (propsRes.isSuccessful) {
                    props = propsRes.body() ?: emptyList()
                    propSpinner.adapter = ArrayAdapter(
                        this@AssignLeaseActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        props.map { it.address ?: it.title ?: "Property ${it.propertyId}" }
                    )
                } else {
                    Toast.makeText(this@AssignLeaseActivity, "Failed to load properties", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(this@AssignLeaseActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
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
            try {
                val response = api.createLease("Bearer $jwt", leaseRequest)
                if (response.isSuccessful) {
                    Toast.makeText(this@AssignLeaseActivity, "Lease assigned successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@AssignLeaseActivity, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AssignLeaseActivity, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
