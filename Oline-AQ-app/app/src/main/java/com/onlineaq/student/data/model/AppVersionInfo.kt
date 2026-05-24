package com.onlineaq.student.data.model

data class AppVersionInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val releaseNotes: String,
    val forceUpdate: Boolean
)
