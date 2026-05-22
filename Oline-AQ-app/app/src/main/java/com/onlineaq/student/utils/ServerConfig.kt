package com.onlineaq.student.utils

import android.content.Context
import android.content.SharedPreferences
import com.onlineaq.student.OnlineAQApp

object ServerConfig {
    private const val PREFS_NAME = "server_prefs"
    private const val KEY_BASE_URL = "base_url"
    const val DEFAULT_URL = "http://39.98.69.153:8084/api/"

    private val prefs: SharedPreferences by lazy {
        OnlineAQApp.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getBaseUrl(): String {
        val saved = prefs.getString(KEY_BASE_URL, null)
        return if (saved.isNullOrBlank()) DEFAULT_URL else saved
    }

    fun setBaseUrl(url: String) {
        val normalized = url.trim().trimEnd('/')
        val finalUrl = if (normalized.endsWith("/api")) "${normalized}/" else normalized
        prefs.edit().putString(KEY_BASE_URL, finalUrl).apply()
    }

    fun resetToDefault() {
        prefs.edit().remove(KEY_BASE_URL).apply()
    }
}
