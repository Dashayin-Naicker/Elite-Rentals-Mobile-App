package com.rentals.eliterentals

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

open class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("app", Context.MODE_PRIVATE)
        val lang = prefs.getString("language", "en") ?: "en"
        val locale = Locale(lang)
        Locale.setDefault(locale)

        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        val localizedContext = newBase.createConfigurationContext(config)
        super.attachBaseContext(localizedContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applySavedTheme() // Apply theme before layout inflation
        super.onCreate(savedInstanceState)
    }

    private fun applySavedTheme() {
        val prefs = getSharedPreferences("app", Context.MODE_PRIVATE)
        val savedTheme = prefs.getString("theme", "light") ?: "light"

        when (savedTheme) {
            "light" -> setTheme(R.style.Theme_EliteRentals_Light)
            "dark" -> setTheme(R.style.Theme_EliteRentals_Dark)
            "high_contrast" -> setTheme(R.style.Theme_EliteRentals_HighContrast)
            else -> setTheme(R.style.Theme_EliteRentals_Light)
        }
    }
}
