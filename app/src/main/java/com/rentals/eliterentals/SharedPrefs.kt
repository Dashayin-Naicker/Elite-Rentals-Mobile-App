package com.rentals.eliterentals

import android.content.Context

object SharedPrefs {

    private const val PREF_NAME = "app"

    // -------------------------
    // Authentication
    // -------------------------
    fun getToken(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString("jwt", "") ?: ""
    }

    fun getUserId(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getInt("userId", 0)
    }

    fun getUserRole(context: Context, userId: Int): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return if (prefs.getInt("userId", 0) == userId) {
            prefs.getString("role", null)
        } else {
            null // Extend this to cache other users' roles if needed
        }
    }

    // -------------------------
    // FCM Token
    // -------------------------
    fun saveFcmToken(context: Context, token: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
        prefs.putString("fcmToken", token)
        prefs.apply()
    }

    fun getFcmToken(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString("fcmToken", "") ?: ""
    }

    // -------------------------
    // Manager Info
    // -------------------------
    fun setManagerId(ctx: Context, id: Int) {
        ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putInt("managerId", id).apply()
    }

    fun getManagerId(ctx: Context): Int {
        return ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getInt("managerId", 0)
    }

    fun setManagerName(ctx: Context, name: String) {
        ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString("managerName", name).apply()
    }

    fun getManagerName(ctx: Context): String? {
        return ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString("managerName", "")
    }

    fun setManagerEmail(ctx: Context, email: String) {
        ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString("managerEmail", email).apply()
    }

    fun getManagerEmail(ctx: Context): String? {
        return ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString("managerEmail", "")
    }

    // -------------------------
    // Theme
    // -------------------------
    fun getTheme(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString("theme", "light") ?: "light"
    }

    fun setTheme(context: Context, theme: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString("theme", theme).apply()
    }
}
