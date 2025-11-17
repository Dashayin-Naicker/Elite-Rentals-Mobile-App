package com.rentals.eliterentals

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

class RegisterUserActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var spinnerRole: Spinner
    private lateinit var icBack: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_user)

        etName = findViewById(R.id.etName)
        etLastName = findViewById(R.id.etLastName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnRegister = findViewById(R.id.btnRegister)
        spinnerRole = findViewById(R.id.spinnerRole)
        icBack = findViewById(R.id.ic_back)

        // Populate role spinner
        val roles = listOf("Admin", "PropertyManager", "Tenant", "Caretaker")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRole.adapter = adapter

        icBack.setOnClickListener { finish() }

        btnRegister.setOnClickListener {
            val first = etName.text.toString().trim()
            val last = etLastName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val role = spinnerRole.selectedItem.toString()

            if (first.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in required fields", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val request = RegisterRequest(
                firstName = first,
                lastName = last,
                email = email,
                password = password,
                role = role
            )

            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    try {
                        val response = RetrofitClient.instance.registerUser(request)
                        if (response.isSuccessful && response.body() != null) {
                            Toast.makeText(this@RegisterUserActivity, "User Registered Successfully!", Toast.LENGTH_SHORT).show()
                            // After success, finish to return to system users list (which will refresh in onResume)
                            finish()
                        } else {
                            Toast.makeText(this@RegisterUserActivity, "Failed: ${response.code()}", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@RegisterUserActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
