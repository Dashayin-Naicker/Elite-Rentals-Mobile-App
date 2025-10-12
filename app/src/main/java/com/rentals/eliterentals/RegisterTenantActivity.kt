package com.rentals.eliterentals

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

class RegisterTenantActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_tenant)

        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnRegister = findViewById(R.id.btnRegister)

        // 🔙 Top bar back -> Dashboard
        findViewById<ImageView>(R.id.ic_back)?.setOnClickListener {
            startActivity(MainPmActivity.createIntent(this, MainPmActivity.Tab.DASHBOARD))
            finish()
        }

        // ✅ Bottom navbar routes (now includes Dashboard)
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
            startActivity(Intent(this, AssignLeaseActivity::class.java))
        }
        findViewById<ImageView>(R.id.navAssignMaintenance).setOnClickListener {
            startActivity(Intent(this, CaretakerTrackMaintenanceActivity::class.java))
        }
        findViewById<ImageView>(R.id.navRegisterTenant).setOnClickListener {
            Toast.makeText(this, "Already on Register Tenant", Toast.LENGTH_SHORT).show()
        }
        findViewById<ImageView>(R.id.navGenerateReport).setOnClickListener {
            Toast.makeText(this, "Coming Soon", Toast.LENGTH_SHORT).show()
        }

        // 📝 Register flow
        btnRegister.setOnClickListener {
            val fullName = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val first = fullName.split(" ").firstOrNull().orEmpty()
            val last = fullName.split(" ").drop(1).joinToString(" ")

            val request = RegisterRequest(
                firstName = first,
                lastName = last,
                email = email,
                password = password
            )

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
                            // Go “home” after success
                            startActivity(MainPmActivity.createIntent(this@RegisterTenantActivity, MainPmActivity.Tab.DASHBOARD))
                            finish()
                        } else {
                            Toast.makeText(this@RegisterTenantActivity, "Failed: ${response.code()}", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@RegisterTenantActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
