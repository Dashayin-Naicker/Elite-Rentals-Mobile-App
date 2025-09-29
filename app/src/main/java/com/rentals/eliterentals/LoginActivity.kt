package com.rentals.eliterentals

import android.content.Intent
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.widget.EditText
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class LoginActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnGoogle: MaterialButton
    private lateinit var btnFingerprint: MaterialButton
    private lateinit var googleSignInClient: GoogleSignInClient

    companion object {
        private const val RC_SIGN_IN = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnGoogle = findViewById(R.id.btnGoogle)
        btnFingerprint = findViewById(R.id.btnFingerprint)

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.server_client_id)) // your WEB client ID
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        btnLogin.setOnClickListener { doNormalLogin() }
        btnGoogle.setOnClickListener { doGoogleLogin() }
        btnFingerprint.setOnClickListener { doFingerprintLogin() }
    }

    // -------------------- Normal Login --------------------
    private fun doNormalLogin() {
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both username and password", Toast.LENGTH_SHORT).show()
            return
        }

        val loginRequest = LoginRequest(username, password)
        RetrofitClient.instance.login(loginRequest).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    if (loginResponse != null) {
                        handleLoginSuccess(loginResponse)
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

    // -------------------- Google SSO --------------------
    private fun doGoogleLogin() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    sendIdTokenToApi(idToken)
                } else {
                    Toast.makeText(this, "No ID Token received", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                Log.e("GoogleLogin", "Google sign in failed", e)
            }
        }
    }

    private fun sendIdTokenToApi(idToken: String) {
        val ssoPayload = SsoLoginRequest(
            provider = "Google",
            token = idToken,
            email = "",
            firstName = "",
            lastName = "",
            role = "Tenant"
        )

        RetrofitClient.instance.ssoLogin(ssoPayload).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    handleLoginSuccess(response.body()!!)
                } else {
                    Toast.makeText(this@LoginActivity, "SSO Login failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Toast.makeText(this@LoginActivity, "Network error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    // -------------------- Fingerprint Login --------------------
    private fun doFingerprintLogin() {
        val executor = ContextCompat.getMainExecutor(this)

        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(this@LoginActivity, "Biometric login successful", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this@LoginActivity, TenantDashboardActivity::class.java)
                    startActivity(intent)
                    finish()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(this@LoginActivity, "Auth error: $errString", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(this@LoginActivity, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Login")
            .setSubtitle("Use fingerprint or face to login")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    // -------------------- Shared Login Success Handler --------------------
    private fun handleLoginSuccess(loginResponse: LoginResponse) {
        Log.d("LoginResponse", "Token: ${loginResponse.token}")
        Log.d("LoginResponse", "User email: ${loginResponse.user.email}")
        Log.d("LoginResponse", "Manager ID: ${loginResponse.user.managerId}")


        val prefs = getSharedPreferences("app", MODE_PRIVATE).edit()
        prefs.putString("jwt", loginResponse.token)
        prefs.putInt("userId", loginResponse.user.userId) // Save tenant/user ID
        prefs.putString("tenantName", loginResponse.user.firstName + " " + loginResponse.user.lastName) // Save name
        loginResponse.user.managerId?.let {
            prefs.putInt("managerId", it)
        }
        prefs.apply()



        Toast.makeText(this@LoginActivity, "Welcome ${loginResponse.user.firstName}", Toast.LENGTH_LONG).show()

        val role = loginResponse.user.role?.trim()
        when (role) {
            "Tenant" -> {
                val intent = Intent(this@LoginActivity, TenantDashboardActivity::class.java)
                intent.putExtra("token", loginResponse.token)
                startActivity(intent)
                finish()
            }
            "Caretaker" -> {
                val intent = Intent(this@LoginActivity, CaretakerTrackMaintenanceActivity::class.java)
                intent.putExtra("token", loginResponse.token)
                startActivity(intent)
                finish()
            }
            "PropertyManager" -> {
                val intent = Intent(this@LoginActivity, MainPmActivity::class.java)
                intent.putExtra("token", loginResponse.token)
                startActivity(intent)
                finish()
            }
            else -> {
                Toast.makeText(this@LoginActivity, "Unknown role: $role", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
