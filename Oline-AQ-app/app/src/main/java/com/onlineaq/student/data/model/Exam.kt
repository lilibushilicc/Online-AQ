package com.onlineaq.student.data.model

data class Exam(
    val examId: Int,
    val examName: String,
    val description: String?,
    val duration: Int,
    val totalScore: Double?,
    val status: String,
    val allowRetake: Boolean?,
    val assignAll: Boolean?,
    val startTime: String?,
    val endTime: String?,
    val createTime: String?
)

data class ExamDetail(
    val exam: Exam,
    val questions: List<Question>
)
