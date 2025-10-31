    package com.rentals.eliterentals

    import android.app.AlertDialog
    import android.content.Context
    import android.content.Intent
    import android.content.SharedPreferences
    import android.os.Bundle
    import android.os.Handler
    import android.os.Looper
    import android.widget.*
    import com.google.android.material.button.MaterialButton
    import okhttp3.OkHttpClient
    import retrofit2.*
    import retrofit2.converter.gson.GsonConverterFactory
    import java.util.*
    import android.content.res.Configuration
    import android.view.View


    class SettingsActivity : BaseActivity() {

        private lateinit var etCurrentPassword: EditText
        private lateinit var etNewPassword: EditText
        private lateinit var btnChangePassword: MaterialButton
        private lateinit var switchBiometric: Switch
        private lateinit var radioGroupTheme: RadioGroup
        private lateinit var btnLogout: MaterialButton
        private lateinit var btnBack: ImageView

        private lateinit var prefs: SharedPreferences
        private lateinit var biometricPrefs: SharedPreferences
        private var userId: Int = -1
        private lateinit var jwt: String
        private lateinit var role: String

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_settings)

            prefs = getSharedPreferences("app", Context.MODE_PRIVATE)
            biometricPrefs = getSharedPreferences("biometric_prefs", Context.MODE_PRIVATE)

            userId = prefs.getInt("userId", -1)
            jwt = prefs.getString("jwt", "") ?: ""
            role = prefs.getString("role", "Tenant") ?: "Tenant"

            // Initialize views
            etCurrentPassword = findViewById(R.id.etCurrentPassword)
            etNewPassword = findViewById(R.id.etNewPassword)
            btnChangePassword = findViewById(R.id.btnChangePassword)
            switchBiometric = findViewById(R.id.switchBiometric)
            radioGroupTheme = findViewById(R.id.radioGroupTheme)
            btnLogout = findViewById(R.id.btnLogout)
            btnBack = findViewById(R.id.btnBack)

            // Back button functionality
            btnBack.setOnClickListener {
                finish()
            }

            // Load saved settings
            switchBiometric.isChecked = biometricPrefs.getBoolean("enabled", false)

            val currentTheme = prefs.getString("theme", "light") ?: "light"
            when (currentTheme) {
                "light" -> radioGroupTheme.check(R.id.radioLight)
                "dark" -> radioGroupTheme.check(R.id.radioDark)
                "high_contrast" -> radioGroupTheme.check(R.id.radioHighContrast)
            }

            // Change Password
            btnChangePassword.setOnClickListener {
                val current = etCurrentPassword.text.toString()
                val new = etNewPassword.text.toString()
                if (current.isEmpty() || new.isEmpty()) {
                    Toast.makeText(this, "Please fill both fields", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val dto = ChangePasswordRequest(current, new)
                val retrofit = buildSecureRetrofit(jwt)
                val api = retrofit.create(ApiService::class.java)

                api.changePassword(userId, dto).enqueue(object : Callback<ApiResponse> {
                    override fun onResponse(
                        call: Call<ApiResponse>,
                        response: Response<ApiResponse>
                    ) {
                        val msg = response.body()?.message ?: "Password updated"
                        Toast.makeText(this@SettingsActivity, msg, Toast.LENGTH_SHORT).show()
                        etCurrentPassword.text.clear()
                        etNewPassword.text.clear()
                    }

                    override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                        Toast.makeText(
                            this@SettingsActivity,
                            "Error: ${t.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            }

            // Biometric Toggle
            switchBiometric.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    biometricPrefs.edit()
                        .putBoolean("enabled", true)
                        .putInt("userId", userId)
                        .putString("jwt", jwt)
                        .putString("role", role)
                        .putString("tenantName", prefs.getString("tenantName", ""))
                        .apply()
                } else {
                    biometricPrefs.edit().clear().apply()
                }
            }

            // Theme Selection
            radioGroupTheme.setOnCheckedChangeListener { _, checkedId ->
                val selectedTheme = when (checkedId) {
                    R.id.radioLight -> "light"
                    R.id.radioDark -> "dark"
                    R.id.radioHighContrast -> "high_contrast"
                    else -> "light"
                }

                prefs.edit().putString("theme", selectedTheme).apply()
                ThemeUtils.applyTheme(selectedTheme)

                val progressBar = ProgressBar(this).apply { isIndeterminate = true }
                val dialog = AlertDialog.Builder(this)
                    .setView(progressBar)
                    .setMessage("Applying theme...")
                    .setCancelable(false)
                    .create()
                dialog.show()

                Handler(Looper.getMainLooper()).postDelayed({
                    if (!isFinishing && !isDestroyed) {
                        try {
                            if (dialog.isShowing) dialog.dismiss()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        recreate()
                    }
                }, 300)
            }

            // Language Spinner Setup
            val spinnerLanguage: Spinner = findViewById(R.id.spinnerLanguage)
            val languageOptions = resources.getStringArray(R.array.language_options)
            val languageAdapter =
                ArrayAdapter(this, android.R.layout.simple_spinner_item, languageOptions)
            languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerLanguage.adapter = languageAdapter

            // Set current selection based on saved locale
            val savedLang = prefs.getString("language", Locale.getDefault().language)
            spinnerLanguage.setSelection(
                when (savedLang) {
                    "zu" -> 1
                    else -> 0
                }
            )

            spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedLang = if (position == 1) "zu" else "en"
                    val savedLang = prefs.getString("language", "en") ?: "en"

                    if (selectedLang != savedLang) {
                        prefs.edit().putString("language", selectedLang).apply()
                        restartApp()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }


            // Logout
            btnLogout.setOnClickListener {
                prefs.edit().clear().apply()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }


        }

        private fun setLocale(languageCode: String) {
            val locale = Locale(languageCode)
            Locale.setDefault(locale)
            val config = Configuration()
            config.setLocale(locale)
            baseContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)
        }


        private fun restartApp() {
            val intent = Intent(this, TenantDashboardActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }

            private fun buildSecureRetrofit(token: String): Retrofit {
                val client = OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .addHeader("Authorization", "Bearer $token")
                            .build()
                        chain.proceed(request)
                    }
                    .build()

                return Retrofit.Builder()
                    .baseUrl("https://eliterentalsapi-czckh7fadmgbgtgf.southafricanorth-01.azurewebsites.net/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build()
            }
        }


