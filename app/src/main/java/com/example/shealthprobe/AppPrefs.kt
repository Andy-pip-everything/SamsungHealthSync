package com.example.shealthprobe

import android.content.Context

object AppPrefs {
    private const val PREFS = "health_sync_prefs"
    private const val KEY_URL = "url"
    private const val KEY_TOKEN = "token"
    private const val KEY_AUTO = "auto"
    private const val KEY_HOUR = "hour"
    private const val KEY_MINUTE = "minute"
    private const val KEY_LAST_STATUS = "last_status"

    fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun url(context: Context): String = prefs(context).getString(KEY_URL, "") ?: ""
    fun token(context: Context): String = prefs(context).getString(KEY_TOKEN, "") ?: ""
    fun autoEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_AUTO, false)
    fun hour(context: Context): Int = prefs(context).getInt(KEY_HOUR, 21)
    fun minute(context: Context): Int = prefs(context).getInt(KEY_MINUTE, 30)
    fun lastStatus(context: Context): String = prefs(context).getString(KEY_LAST_STATUS, "Not synced yet") ?: "Not synced yet"

    fun saveConfig(context: Context, url: String, token: String, auto: Boolean, hour: Int, minute: Int) {
        prefs(context).edit()
            .putString(KEY_URL, url.trim())
            .putString(KEY_TOKEN, token.trim())
            .putBoolean(KEY_AUTO, auto)
            .putInt(KEY_HOUR, hour)
            .putInt(KEY_MINUTE, minute)
            .apply()
    }

    fun setLastStatus(context: Context, status: String) {
        prefs(context).edit().putString(KEY_LAST_STATUS, status).apply()
    }
}
