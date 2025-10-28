package com.rentals.eliterentals

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

open class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply the theme BEFORE super.onCreate
        applySavedTheme()
        super.onCreate(savedInstanceState)
    }

    override fun attachBaseContext(newBase: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(newBase)
        val lang = prefs.getString("language", "en") ?: "en"
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(Locale(lang))
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
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