package com.rentals.eliterentals

import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import android.widget.Toast

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
        adapter = TenantAdapter(mutableListOf(), ::onApproveClicked)
        rv.adapter = adapter

        jwt = getSharedPreferences("app", MODE_PRIVATE).getString("jwt", "") ?: ""

        fetchTenants()
    }

    private fun fetchTenants() {
        lifecycleScope.launch {
            val res = api.getAllUsers("Bearer $jwt")
            if (res.isSuccessful) {
                val tenants = res.body()?.filter { it.role == "Tenant" } ?: emptyList()
                adapter.submit(tenants)
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
}
