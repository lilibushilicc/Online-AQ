package com.onlineaq.student.utils

import android.content.Context
import android.content.SharedPreferences
import com.onlineaq.student.OnlineAQApp

object TokenManager {
    private const val PREFS_NAME = "online_aq_prefs"
    private const val KEY_TOKEN = "token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USERNAME = "username"
    private const val KEY_REAL_NAME = "real_name"
    private const val KEY_ROLE = "role"

    private val prefs: SharedPreferences by lazy {
        OnlineAQApp.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveLoginData(token: String, userId: Int, username: String, realName: String, role: String) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_USERNAME, username)
            .putString(KEY_REAL_NAME, realName)
            .putString(KEY_ROLE, role)
            .apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)
    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, 0)
    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)
    fun getRealName(): String? = prefs.getString(KEY_REAL_NAME, null)
    fun getRole(): String? = prefs.getString(KEY_ROLE, null)

    fun isLoggedIn(): Boolean = getToken() != null

    fun clear() {
        prefs.edit().clear().apply()
    }
}
