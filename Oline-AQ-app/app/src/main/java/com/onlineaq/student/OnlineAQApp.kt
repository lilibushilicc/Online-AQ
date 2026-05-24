package com.onlineaq.student

import android.app.Application
import com.onlineaq.student.data.api.RetrofitClient
import com.onlineaq.student.data.model.AppVersionInfo
import com.onlineaq.student.utils.ThemeConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class OnlineAQApp : Application() {
    companion object {
        lateinit var instance: OnlineAQApp
            private set

        var latestVersion: AppVersionInfo? = null
            private set
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        ThemeConfig.applyThemeMode()
        checkAppVersion()
    }

    private fun checkAppVersion() {
        appScope.launch {
            try {
                val response = RetrofitClient.apiService.getLatestAppVersion()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.code == 200 && body.data != null) {
                        latestVersion = body.data
                    }
                }
            } catch (_: Exception) {
                // 网络或服务器不可用时静默忽略
            }
        }
    }
}
