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

    fun saveFcmToken(context: Context, token: String) {
        val prefs = context.getSharedPreferences("app", Context.MODE_PRIVATE).edit()
        prefs.putString("fcmToken", token)
        prefs.apply()
    }

    fun getFcmToken(context: Context): String {
        val prefs = context.getSharedPreferences("app", Context.MODE_PRIVATE)
        return prefs.getString("fcmToken", "") ?: ""
    }

    fun setManagerId(ctx: Context, id: Int) =
        ctx.getSharedPreferences("app", Context.MODE_PRIVATE).edit().putInt("managerId", id).apply()
    fun getManagerId(ctx: Context) =
        ctx.getSharedPreferences("app", Context.MODE_PRIVATE).getInt("managerId", 0)

    fun setManagerName(ctx: Context, v: String) =
        ctx.getSharedPreferences("app", Context.MODE_PRIVATE).edit().putString("managerName", v).apply()
    fun getManagerName(ctx: Context) =
        ctx.getSharedPreferences("app", Context.MODE_PRIVATE).getString("managerName", "")

    fun setManagerEmail(ctx: Context, v: String) =
        ctx.getSharedPreferences("app", Context.MODE_PRIVATE).edit().putString("managerEmail", v).apply()
    fun getManagerEmail(ctx: Context) =
        ctx.getSharedPreferences("app", Context.MODE_PRIVATE).getString("managerEmail", "")


    fun getTheme(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString("theme", "light") ?: "light"
    }

    fun setTheme(context: Context, theme: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString("theme", theme).apply()
    }

}

