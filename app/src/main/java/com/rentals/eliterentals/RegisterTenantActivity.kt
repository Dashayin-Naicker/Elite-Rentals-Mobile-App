package com.rentals.eliterentals

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.widget.Toast

class RegisterTenantActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_tenant)

        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btn = findViewById<Button>(R.id.btnRegister)

        btn.setOnClickListener {
            val fullName = etName.text.toString().trim()
            val first = fullName.split(" ").firstOrNull() ?: ""
            val last = fullName.split(" ").drop(1).joinToString(" ")
            val req = RegisterRequest(first, last, etEmail.text.toString(), etPassword.text.toString())

            lifecycleScope.launch {
                val res = RetrofitClient.instance.registerUser(req)
                if (res.isSuccessful) {
                    Toast.makeText(this@RegisterTenantActivity, "Tenant Registered!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@RegisterTenantActivity, "Failed: ${res.code()}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
