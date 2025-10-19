package com.rentals.eliterentals

import android.content.Context

object SharedPrefs {
    private const val PREF_NAME = "app"

    fun getToken(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString("jwt", "") ?: ""
    }

    fun getUserId(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getInt("userId", 0)
    }

    fun getUserRole(context: Context, userId: Int): String? {
        val prefs = context.getSharedPreferences("app", Context.MODE_PRIVATE)
        return if (prefs.getInt("userId", 0) == userId) {
            prefs.getString("role", null)
        } else {
            null // You can extend this to cache other users' roles if needed
        }
    }

}

