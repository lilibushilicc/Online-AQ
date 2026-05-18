package com.onlineaq.student.data.model

data class Question(
    val questionId: Int,
    val questionContent: String,
    val questionType: String,
    val optionA: String?,
    val optionB: String?,
    val optionC: String?,
    val optionD: String?,
    val correctAnswer: String?,
    val score: Double?,
    val category: String?
)
