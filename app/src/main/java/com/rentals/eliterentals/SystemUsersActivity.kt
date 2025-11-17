package com.rentals.eliterentals

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class SystemUsersActivity : AppCompatActivity() {

    private lateinit var rv: RecyclerView
    private lateinit var adapter: SystemUsersAdapter
    private lateinit var btnAddNewUser: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_system_users)

        rv = findViewById(R.id.rvSystemUsers)
        btnAddNewUser = findViewById(R.id.btnAddNewUser)

        adapter = SystemUsersAdapter(mutableListOf(), onApprove = { user ->
            approveUser(user)
        }, onToggle = { user ->
            toggleUser(user)
        })

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        btnAddNewUser.setOnClickListener {
            // Launch RegisterUserActivity to add any role
            startActivity(Intent(this@SystemUsersActivity, RegisterUserActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        fetchUsers()
    }

    private fun fetchUsers() {
        val jwt = SharedPrefs.getToken(this)
        val api = RetrofitClient.instance

        lifecycleScope.launch {
            try {
                val res = api.getAllUsers("Bearer $jwt")

                if (res.isSuccessful) {
                    val list = res.body()?.filterNotNull() ?: emptyList()
                    adapter.submit(list)
                } else {
                    Toast.makeText(
                        this@SystemUsersActivity,
                        "Failed to load users: ${res.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@SystemUsersActivity,
                    "Error: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun approveUser(user: UserDto) {
        // Build full User object and call updateUser to set tenantApproval = "Approved"
        val fullUser = User(
            userId = user.userId,
            firstName = user.firstName ?: "",
            lastName = user.lastName ?: "",
            email = user.email ?: "",
            role = user.role ?: "Tenant",
            tenantApproval = "Approved",
            isActive = user.isActive
        )
        val jwt = SharedPrefs.getToken(this)
        val api = RetrofitClient.instance

        lifecycleScope.launch {
            try {
                val res = api.updateUser("Bearer $jwt", user.userId, fullUser)
                if (res.isSuccessful) {
                    Toast.makeText(this@SystemUsersActivity, "Approved ${user.email}", Toast.LENGTH_SHORT).show()
                    fetchUsers()
                } else {
                    Toast.makeText(this@SystemUsersActivity, "Failed to approve: ${res.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SystemUsersActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun toggleUser(user: UserDto) {
        val jwt = SharedPrefs.getToken(this)
        val api = RetrofitClient.instance
        lifecycleScope.launch {
            try {
                val res = api.toggleUserStatus("Bearer $jwt", user.userId)
                if (res.isSuccessful) {
                    val msg = if (user.isActive) "Disabled ${user.email}" else "Enabled ${user.email}"
                    Toast.makeText(this@SystemUsersActivity, msg, Toast.LENGTH_SHORT).show()
                    fetchUsers()
                } else {
                    Toast.makeText(this@SystemUsersActivity, "Failed to toggle status: ${res.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SystemUsersActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
