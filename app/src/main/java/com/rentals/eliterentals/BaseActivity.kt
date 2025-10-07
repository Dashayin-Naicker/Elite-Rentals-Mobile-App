package com.rentals.eliterentals

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply the theme BEFORE super.onCreate
        applySavedTheme()
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