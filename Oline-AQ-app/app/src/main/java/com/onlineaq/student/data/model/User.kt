package com.onlineaq.student.data.model

data class LoginData(
    val token: String,
    val userId: Int,
    val username: String,
    val realName: String,
    val role: String
)
