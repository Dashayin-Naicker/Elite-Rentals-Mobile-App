package com.rentals.eliterentals

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import android.widget.Button
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle

class TenantListActivity : AppCompatActivity() {
    private lateinit var rv: RecyclerView
    private lateinit var adapter: TenantAdapter
    private val api = RetrofitClient.instance
    private var jwt = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tenant_list)

        rv = findViewById(R.id.rvTenants)
        rv.layoutManager = LinearLayoutManager(this)

        adapter = TenantAdapter(mutableListOf(), ::onApproveClicked, ::onToggleClicked)
        rv.adapter = adapter

        jwt = getSharedPreferences("app", MODE_PRIVATE).getString("jwt", "") ?: ""
        fetchTenants()
    }

    private fun fetchTenants() {
        lifecycleScope.launch {
            try {
                val res = api.getAllUsers("Bearer $jwt")
                if (res.isSuccessful) {
                    // Filter nulls and only tenants
                    val tenants = res.body()?.filterNotNull()?.filter { it.role == "Tenant" } ?: emptyList()
                    adapter.submit(tenants)
                } else {
                    Toast.makeText(
                        this@TenantListActivity,
                        "Failed to load tenants: ${res.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@TenantListActivity,
                    "Network error: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    private fun onApproveClicked(user: UserDto) {
        val fullUser = User(
            userId = user.userId,
            firstName = user.firstName ?: "",
            lastName = user.lastName ?: "",
            email = user.email ?: "",
            role = user.role ?: "Tenant",
            tenantApproval = "Approved",
            isActive = user.isActive
        )

        lifecycleScope.launch {
            try {
                val res = api.updateUser("Bearer $jwt", user.userId, fullUser)
                if (res.isSuccessful) {
                    Toast.makeText(this@TenantListActivity, "Approved ${user.email}", Toast.LENGTH_SHORT).show()
                    fetchTenants()
                } else {
                    Toast.makeText(this@TenantListActivity, "Failed to approve", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@TenantListActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun onToggleClicked(user: UserDto) {
        lifecycleScope.launch {
            try {
                val res = api.toggleUserStatus("Bearer $jwt", user.userId)
                if (res.isSuccessful) {
                    val msg = if (user.isActive) "Disabled ${user.email}" else "Enabled ${user.email}"
                    Toast.makeText(this@TenantListActivity, msg, Toast.LENGTH_SHORT).show()
                    fetchTenants()
                } else {
                    Toast.makeText(this@TenantListActivity, "Failed to toggle status: ${res.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@TenantListActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        fetchTenants()
    }
}

