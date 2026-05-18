package com.onlineaq.student.data.model

data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T?
)
