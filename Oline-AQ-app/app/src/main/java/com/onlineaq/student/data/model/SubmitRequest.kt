package com.onlineaq.student.data.model

data class SubmitExamRequest(
    val attemptId: String? = null,
    val useTime: Int,
    val answers: List<AnswerItem>
)

data class AnswerItem(
    val questionId: Int,
    val studentAnswer: String
)

data class SubmitResult(
    val resultId: Int,
    val totalScore: Double,
    val correctCount: Int,
    val wrongCount: Int
)
