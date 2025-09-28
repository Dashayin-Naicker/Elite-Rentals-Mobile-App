package com.rentals.eliterentals

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.widget.Button
class AssignLeaseActivity : AppCompatActivity() {
    private lateinit var tenantSpinner: Spinner
    private lateinit var propSpinner: Spinner
    private lateinit var startEt: EditText
    private lateinit var endEt: EditText
    private lateinit var depositEt: EditText
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
        val btnAssign = findViewById<Button>(R.id.btnAssign)

        jwt = getSharedPreferences("app", MODE_PRIVATE).getString("jwt", "") ?: ""
        fetchData()

        btnAssign.setOnClickListener {
            val tenantId = tenants[tenantSpinner.selectedItemPosition].userId
            val propId = props[propSpinner.selectedItemPosition].propertyId
            val req = CreateLeaseRequest(
                propertyId = propId,
                tenantId = tenantId,
                startDate = startEt.text.toString(),
                endDate = endEt.text.toString(),
                deposit = depositEt.text.toString().toDouble()
            )

            lifecycleScope.launch {
                val res = api.createLease("Bearer $jwt", req)
                if (res.isSuccessful) {
                    Toast.makeText(this@AssignLeaseActivity, "Lease created!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun fetchData() {
        lifecycleScope.launch {
            try {
                val u = api.getAllUsers("Bearer $jwt")
                val p = api.getAllProperties("Bearer $jwt") // Pass the JWT here!

                if (u.isSuccessful) {
                    tenants = u.body()?.filter { it.role == "Tenant" && it.tenantApproval == "Approved" } ?: emptyList()
                    tenantSpinner.adapter = ArrayAdapter(
                        this@AssignLeaseActivity,
                        android.R.layout.simple_spinner_item,
                        tenants.map { "${it.firstName} ${it.lastName}" }
                    )
                }

                if (p.isSuccessful) {
                    props = p.body() ?: emptyList()
                    propSpinner.adapter = ArrayAdapter(
                        this@AssignLeaseActivity,
                        android.R.layout.simple_spinner_item,
                        props.map {
                            val addr = it.address ?: ""              // handle null
                            if (addr.isNotEmpty()) addr else it.title ?: "Property ${it.propertyId}"
                        }
                    )
                }
 else {
                    Toast.makeText(this@AssignLeaseActivity, "Failed to load properties", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AssignLeaseActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

}
