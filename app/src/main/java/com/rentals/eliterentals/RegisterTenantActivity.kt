package com.rentals.eliterentals

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

class RegisterTenantActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_tenant)

        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        btnRegister.setOnClickListener {
            val fullName = etName.text.toString().trim()
            if (fullName.isEmpty() || etEmail.text.isBlank() || etPassword.text.isBlank()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val first = fullName.split(" ").firstOrNull() ?: ""
            val last = fullName.split(" ").drop(1).joinToString(" ")

            val request = RegisterRequest(
                firstName = first,
                lastName = last,
                email = etEmail.text.toString().trim(),
                password = etPassword.text.toString().trim()
            )

            // Use coroutine to call API
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    try {
                        val response = RetrofitClient.instance.registerUser(request)
                        if (response.isSuccessful && response.body() != null) {
                            Toast.makeText(
                                this@RegisterTenantActivity,
                                "Tenant Registered Successfully!",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish() // Close activity after successful registration
                        } else {
                            val code = response.code()
                            val message = response.errorBody()?.string()
                            Toast.makeText(
                                this@RegisterTenantActivity,
                                "Failed: $code\n$message",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@RegisterTenantActivity,
                            "Error: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        // Bottom Navigation Clicks
        findViewById<ImageView>(R.id.navManageProperties).setOnClickListener {
            navigateToActivity(AddEditPropertyActivity::class.java)
        }

        findViewById<ImageView>(R.id.navManageTenants).setOnClickListener {
            navigateToFragment(TenantsFragment())
        }

        findViewById<ImageView>(R.id.navAssignLeases).setOnClickListener {
            navigateToActivity(AssignLeaseActivity::class.java)
        }

        findViewById<ImageView>(R.id.navAssignMaintenance).setOnClickListener {
            navigateToActivity(CaretakerTrackMaintenanceActivity::class.java)
        }

        findViewById<ImageView>(R.id.navRegisterTenant).setOnClickListener {
            Toast.makeText(this, "Already on Register Tenant", Toast.LENGTH_SHORT).show()
        }

        findViewById<ImageView>(R.id.navGenerateReport).setOnClickListener {
            Toast.makeText(this, "Coming Soon", Toast.LENGTH_SHORT).show()
        }
    }

    // Navigate to another Activity
    private fun navigateToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
    }
    private fun navigateToFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
