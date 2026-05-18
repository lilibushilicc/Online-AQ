package com.onlineaq.student

import android.app.Application

class OnlineAQApp : Application() {
    companion object {
        const val BASE_URL = "http://10.0.2.2:8080/api/"
        lateinit var instance: OnlineAQApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
