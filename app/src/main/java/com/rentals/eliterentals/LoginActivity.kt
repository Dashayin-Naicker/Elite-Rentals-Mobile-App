package com.rentals.eliterentals

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.widget.EditText

class LoginActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnGoogle: MaterialButton
    private lateinit var btnFingerprint: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnGoogle = findViewById(R.id.btnGoogle)
        btnFingerprint = findViewById(R.id.btnFingerprint)

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val loginRequest = LoginRequest(username, password)
            RetrofitClient.instance.login(loginRequest).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful) {
                        val loginResponse = response.body()
                        if (loginResponse != null) {
                            Log.d("LoginResponse", "Token: ${loginResponse.token}")
                            Log.d("LoginResponse", "User email: ${loginResponse.user.email}")

                            Toast.makeText(this@LoginActivity, "Welcome ${loginResponse.user.firstName}", Toast.LENGTH_LONG).show()



                            // TODO: Save token and navigate to dashboard
                        } else {
                            Toast.makeText(this@LoginActivity, "Empty response from server", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@LoginActivity, "Invalid credentials", Toast.LENGTH_SHORT).show()
                    }
                } 

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Toast.makeText(this@LoginActivity, "Network error: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
        }

        btnGoogle.setOnClickListener {
            Toast.makeText(this, "Google login not implemented yet", Toast.LENGTH_SHORT).show()
        }

        btnFingerprint.setOnClickListener {
            Toast.makeText(this, "Fingerprint login not implemented yet", Toast.LENGTH_SHORT).show()
        }
    }
}
