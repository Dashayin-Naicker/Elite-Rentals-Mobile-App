package com.rentals.eliterentals

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(ThemeUtils.getSavedTheme(this))
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        when (prefs.getString("theme", "light")) {
            "light" -> setTheme(R.style.Theme_EliteRentals_Light)
            "dark" -> setTheme(R.style.Theme_EliteRentals_Dark)
            "high_contrast" -> setTheme(R.style.Theme_EliteRentals_HighContrast)
        }
        super.onCreate(savedInstanceState)
    }
}
