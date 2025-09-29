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

        val btnAddTenant = findViewById<Button>(R.id.btnAddTenant)
        btnAddTenant.setOnClickListener {
            val intent = Intent(this, RegisterTenantActivity::class.java)
            startActivity(intent)
        }
    }

    private fun fetchTenants() {
        lifecycleScope.launch {
            val res = api.getAllUsers("Bearer $jwt")
            if (res.isSuccessful) {
                val tenants = res.body()?.filter { it.role == "Tenant" } ?: emptyList()
                adapter.submit(tenants)
            } else {
                Toast.makeText(this@TenantListActivity, "Failed to load tenants", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onApproveClicked(user: UserDto) {
        lifecycleScope.launch {
            val res = api.updateUser("Bearer $jwt", user.userId, UserUpdateDto("Approved"))
            if (res.isSuccessful) {
                Toast.makeText(this@TenantListActivity, "Approved ${user.email}", Toast.LENGTH_SHORT).show()
                fetchTenants()
            }
        }
    }

    private fun onToggleClicked(user: UserDto) {
        lifecycleScope.launch {
            val res = api.toggleUserStatus("Bearer $jwt", user.userId)
            if (res.isSuccessful) {
                Toast.makeText(this@TenantListActivity, "Toggled ${user.email}", Toast.LENGTH_SHORT).show()
                fetchTenants()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        fetchTenants()
    }

}
