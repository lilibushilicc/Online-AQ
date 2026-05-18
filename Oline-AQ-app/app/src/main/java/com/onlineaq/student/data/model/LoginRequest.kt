package com.onlineaq.student.data.model

data class LoginRequest(
    val username: String,
    val password: String,
    val role: String = "student"
)
