package com.rentals.eliterentals

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeUtils {

    fun applyTheme(theme: String) {
        when (theme) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "high_contrast" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    fun saveTheme(context: Context, theme: String) {
        // Use "app" to match SettingsActivity, not "settings"
        context.getSharedPreferences("app", Context.MODE_PRIVATE)
            .edit()
            .putString("theme", theme)
            .apply()
    }

    fun getSavedTheme(context: Context): String {
        // Use "app" to match SettingsActivity, not "settings"
        return context.getSharedPreferences("app", Context.MODE_PRIVATE)
            .getString("theme", "light") ?: "light"
    }
}