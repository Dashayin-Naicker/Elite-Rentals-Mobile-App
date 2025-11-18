package com.rentals.eliterentals

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.messaging.FirebaseMessaging
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LoginActivity : BaseActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnGoogle: MaterialButton
    private lateinit var btnFingerprint: MaterialButton
    private lateinit var googleSignInClient: GoogleSignInClient

    companion object {
        private const val RC_SIGN_IN = 1001
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnGoogle = findViewById(R.id.btnGoogle)
        btnFingerprint = findViewById(R.id.btnFingerprint)

        // -------- GoogleSignInOptions ----------
        // Make sure R.string.server_client_id contains your WEB client ID:
        // "xxxxx.apps.googleusercontent.com"
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.server_client_id)) // Web client ID (OAuth 2.0)
            .requestEmail()
            .requestProfile()
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
            Toast.makeText(this, getString(R.string.login_missing_fields), Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(this@LoginActivity, getString(R.string.login_empty_response), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@LoginActivity, getString(R.string.login_invalid_credentials), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Toast.makeText(this@LoginActivity, getString(R.string.error_network_with_message, t.message), Toast.LENGTH_LONG).show()
            }
        })
    }

    // -------------------- Google SSO --------------------
    private fun doGoogleLogin() {
        // Force sign out and revoke access to avoid silent sign in / cached account selection.
        // This ensures the account picker appears.
        googleSignInClient.signOut().addOnCompleteListener {
            googleSignInClient.revokeAccess().addOnCompleteListener {
                try {
                    val signInIntent = googleSignInClient.signInIntent
                    startActivityForResult(signInIntent, RC_SIGN_IN)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start sign-in intent", e)
                    Toast.makeText(this, getString(R.string.login_sso_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (!idToken.isNullOrEmpty()) {
                    // pass useful profile fields to backend as well
                    val email = account.email ?: ""
                    val firstName = account.givenName ?: ""
                    val lastName = account.familyName ?: ""
                    sendIdTokenToApi(idToken, email, firstName, lastName)
                } else {
                    Toast.makeText(this, getString(R.string.login_no_id_token), Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                Log.e(TAG, "Google sign in failed: ${e.statusCode}", e)
                Toast.makeText(this, getString(R.string.login_sso_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendIdTokenToApi(idToken: String, email: String = "", firstName: String = "", lastName: String = "") {
        val ssoPayload = SsoLoginRequest(
            provider = "Google",
            token = idToken,
            email = email,
            firstName = firstName,
            lastName = lastName,
            role = "Tenant"
        )

        RetrofitClient.instance.ssoLogin(ssoPayload).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    handleLoginSuccess(response.body()!!)
                } else {
                    Toast.makeText(this@LoginActivity, getString(R.string.login_sso_failed), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Toast.makeText(this@LoginActivity, getString(R.string.error_network_with_message, t.message), Toast.LENGTH_LONG).show()
            }
        })
    }

    // -------------------- Fingerprint Login --------------------
    private fun doFingerprintLogin() {
        val executor = ContextCompat.getMainExecutor(this)

        val biometricPrefs = getSharedPreferences("biometric_prefs", MODE_PRIVATE)
        val enabled = biometricPrefs.getBoolean("enabled", false)
        val userId = biometricPrefs.getInt("userId", -1)
        val jwt = biometricPrefs.getString("jwt", null)
        val role = biometricPrefs.getString("role", "Tenant") ?: "Tenant"

        if (!enabled || userId == -1 || jwt == null) {
            Toast.makeText(this, getString(R.string.error_login_required), Toast.LENGTH_SHORT).show()
            return
        }

        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(this@LoginActivity, getString(R.string.biometric_success), Toast.LENGTH_SHORT).show()

                    val appPrefs = getSharedPreferences("app", MODE_PRIVATE)
                    val lang = appPrefs.getString("language", "en")
                    val theme = appPrefs.getString("theme", "light")

                    appPrefs.edit().clear().apply()
                    appPrefs.edit()
                        .putString("language", lang)
                        .putString("theme", theme)
                        .putInt("userId", userId)
                        .putString("jwt", jwt)
                        .putString("role", role)
                        .apply()

                    // Navigate to the correct dashboard based on role
                    val intent = when (role) {
                        "Tenant" -> Intent(this@LoginActivity, TenantDashboardActivity::class.java)
                        "Caretaker" -> Intent(this@LoginActivity, CaretakerTrackMaintenanceActivity::class.java)
                        "PropertyManager" -> Intent(this@LoginActivity, MainPmActivity::class.java)
                        "Admin" -> Intent(this@LoginActivity, AdminDashboardActivity::class.java)
                        else -> Intent(this@LoginActivity, LoginActivity::class.java)
                    }

                    startActivity(intent)
                    finish()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(this@LoginActivity, getString(R.string.biometric_error, errString), Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(this@LoginActivity, getString(R.string.biometric_failed), Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_title))
            .setSubtitle(getString(R.string.biometric_subtitle))
            .setNegativeButtonText(getString(R.string.cancel))
            .build()

        biometricPrompt.authenticate(promptInfo)
    }


    // -------------------- Shared Login Success Handler --------------------
    private fun handleLoginSuccess(loginResponse: LoginResponse) {
        Log.d("LoginResponse", "Token: ${loginResponse.token}")
        Log.d("LoginResponse", "User email: ${loginResponse.user.email}")
        Log.d("LoginResponse", "Manager ID: ${loginResponse.user.managerId}")

        val appPrefs = getSharedPreferences("app", MODE_PRIVATE)
        val lang = appPrefs.getString("language", "en")
        val theme = appPrefs.getString("theme", "light")

        appPrefs.edit().clear().apply()
        appPrefs.edit()
            .putString("language", lang)
            .putString("theme", theme)
            .putString("jwt", loginResponse.token)
            .putInt("userId", loginResponse.user.userId)
            .putString("tenantName", "${loginResponse.user.firstName} ${loginResponse.user.lastName}")
            .putString("role", loginResponse.user.role?.trim() ?: "Tenant")
            .putBoolean("biometric_enabled", true)
            .apply()

        // Save only managerId (fields exist in your DTO)
        loginResponse.user.managerId?.let { appPrefs.edit().putInt("managerId", it).apply() }

        SyncScheduler.scheduleSync(applicationContext, loginResponse.token)

        Toast.makeText(this@LoginActivity, getString(R.string.welcome_user, loginResponse.user.firstName), Toast.LENGTH_LONG).show()

        val role = loginResponse.user.role?.trim()
        val intent = when (role) {
            "Tenant" -> Intent(this, TenantDashboardActivity::class.java)
            "Caretaker" -> Intent(this, CaretakerTrackMaintenanceActivity::class.java)
            "PropertyManager" -> Intent(this, MainPmActivity::class.java)
            "Admin" -> Intent(this, AdminDashboardActivity::class.java)
            else -> {
                Toast.makeText(this, getString(R.string.unknown_role, role), Toast.LENGTH_SHORT).show()
                return
            }
        }

        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            Log.d("FCM", "Sending token after login: $token")
            val api = Retrofit.Builder()
                .baseUrl("https://eliterentalsapi-czckh7fadmgbgtgf.southafricanorth-01.azurewebsites.net/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)

            val prefs = getSharedPreferences("app", MODE_PRIVATE)
            val userId = prefs.getInt("userId", -1)
            val jwtToken = prefs.getString("jwt", null)

            if (userId != -1 && jwtToken != null) {
                api.updateFcmToken("Bearer $jwtToken", userId, FcmTokenRequest(token))
                    .enqueue(object : Callback<Void> {
                        override fun onResponse(call: Call<Void>, response: Response<Void>) {
                            Log.d("FCM", "✅ Token sent after login: ${response.code()}")
                        }

                        override fun onFailure(call: Call<Void>, t: Throwable) {
                            Log.e("FCM", "❌ Token send failed: ${t.message}")
                        }
                    })
            }
        }

        intent.putExtra("token", loginResponse.token)
        intent.putExtra("userId", loginResponse.user.userId)
        startActivity(intent)
        finish()
    }
}
