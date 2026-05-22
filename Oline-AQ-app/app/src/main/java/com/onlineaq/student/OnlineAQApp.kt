package com.onlineaq.student

import android.app.Application
import com.onlineaq.student.utils.ThemeConfig

class OnlineAQApp : Application() {
    companion object {
        lateinit var instance: OnlineAQApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        ThemeConfig.applyThemeMode()
    }
}
