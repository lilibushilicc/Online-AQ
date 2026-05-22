package com.onlineaq.student.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import com.onlineaq.student.OnlineAQApp

object ThemeConfig {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_AMOLED = "amoled_mode"

    const val MODE_SYSTEM = 0
    const val MODE_LIGHT = 1
    const val MODE_DARK = 2

    private val prefs: SharedPreferences by lazy {
        OnlineAQApp.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getThemeMode(): Int = prefs.getInt(KEY_THEME_MODE, MODE_SYSTEM)

    fun setThemeMode(mode: Int) {
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply()
        applyThemeMode(mode)
    }

    fun isAmoled(): Boolean = prefs.getBoolean(KEY_AMOLED, false)

    fun setAmoled(amoled: Boolean) {
        prefs.edit().putBoolean(KEY_AMOLED, amoled).apply()
    }

    fun applyThemeMode(mode: Int = getThemeMode()) {
        when (mode) {
            MODE_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            MODE_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}
