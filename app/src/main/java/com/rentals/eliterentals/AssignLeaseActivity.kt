package com.rentals.eliterentals

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AssignLeaseActivity : AppCompatActivity() {

    private lateinit var tenantSpinner: Spinner
    private lateinit var propSpinner: Spinner
    private lateinit var startEt: EditText
    private lateinit var endEt: EditText
    private lateinit var depositEt: EditText
    private lateinit var btnAssign: Button
    private lateinit var icBack: ImageView

    private val api = RetrofitClient.instance
    private var jwt = ""
    private var tenants = listOf<UserDto>()
    private var props = listOf<PropertyDto>()

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assign_lease)

        // Views
        tenantSpinner = findViewById(R.id.tenantSpinner)
        propSpinner = findViewById(R.id.propSpinner)
        startEt = findViewById(R.id.startEt)
        endEt = findViewById(R.id.endEt)
        depositEt = findViewById(R.id.depositEt)
        btnAssign = findViewById(R.id.btnAssign)
        icBack = findViewById(R.id.ic_back)

        jwt = getSharedPreferences("app", MODE_PRIVATE).getString("jwt", "") ?: ""

        // Top bar back -> Dashboard
        icBack.setOnClickListener {
            startActivity(MainPmActivity.createIntent(this, MainPmActivity.Tab.DASHBOARD))
            finish()
        }

        // Date pickers
        startEt.setOnClickListener { showDatePicker(startEt) }
        endEt.setOnClickListener { showDatePicker(endEt) }

        // Data
        fetchData()

        // Save
        btnAssign.setOnClickListener { assignLease() }

        // Bottom navbar
        findViewById<ImageView>(R.id.navDashboard).setOnClickListener {
            startActivity(MainPmActivity.createIntent(this, MainPmActivity.Tab.DASHBOARD))
            finish()
        }
        findViewById<ImageView>(R.id.navManageProperties).setOnClickListener {
            startActivity(MainPmActivity.createIntent(this, MainPmActivity.Tab.PROPERTIES))
            finish()
        }
        findViewById<ImageView>(R.id.navManageTenants).setOnClickListener {
            startActivity(MainPmActivity.createIntent(this, MainPmActivity.Tab.TENANTS))
            finish()
        }
        findViewById<ImageView>(R.id.navAssignLeases).setOnClickListener {
            Toast.makeText(this, "Already on Assign Lease", Toast.LENGTH_SHORT).show()
        }
        findViewById<ImageView>(R.id.navAssignMaintenance).setOnClickListener {
            startActivity(Intent(this, CaretakerTrackMaintenanceActivity::class.java))
            finish()
        }
        findViewById<ImageView>(R.id.navRegisterTenant).setOnClickListener {
            startActivity(Intent(this, RegisterTenantActivity::class.java))
            finish()
        }
        findViewById<ImageView>(R.id.navGenerateReport).setOnClickListener {
            Toast.makeText(this, "Coming Soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDatePicker(target: EditText) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, day)
                target.setText(dateFmt.format(cal.time))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun fetchData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                try {
                    val tenantsRes = try { api.getAllUsers("Bearer $jwt") } catch (_: Exception) { null }
                    val propsRes = try { api.getAllProperties("Bearer $jwt") } catch (_: Exception) { null }
                    val leasesRes = try { api.getAllLeases("Bearer $jwt") } catch (_: Exception) { null }

                    val leasedTenantIds = leasesRes?.body()?.mapNotNull { it.tenantId }?.toSet() ?: emptySet()

                    tenants = tenantsRes?.body()
                        ?.filterNotNull()
                        ?.filter { it.role == "Tenant" && it.tenantApproval == "Approved" && it.userId !in leasedTenantIds }
                        ?: emptyList()

                    tenantSpinner.adapter = ArrayAdapter(
                        this@AssignLeaseActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        tenants.map { "${it.firstName.orEmpty()} ${it.lastName.orEmpty()}".trim() }
                    )

                    props = propsRes?.body()?.filterNotNull()?.filter { it.status == "Available" } ?: emptyList()
                    propSpinner.adapter = ArrayAdapter(
                        this@AssignLeaseActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        props.map { it.address ?: it.title ?: "Property ${it.propertyId}" }
                    )

                    btnAssign.isEnabled = tenants.isNotEmpty() && props.isNotEmpty()
                    if (!btnAssign.isEnabled) {
                        Toast.makeText(
                            this@AssignLeaseActivity,
                            "No available tenants or properties.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
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

                        val statusUpdate = PropertyStatusDto("Occupied")
                        val statusRes = api.updatePropertyStatus("Bearer $jwt", propId, statusUpdate)
                        if (!statusRes.isSuccessful) {
                            Toast.makeText(this@AssignLeaseActivity, "Failed to update property status", Toast.LENGTH_SHORT).show()
                        }

                        // After success -> Dashboard
                        startActivity(MainPmActivity.createIntent(this@AssignLeaseActivity, MainPmActivity.Tab.DASHBOARD))
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
