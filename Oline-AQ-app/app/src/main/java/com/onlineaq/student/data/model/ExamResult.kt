package com.onlineaq.student.data.model

data class ExamResult(
    val resultId: Int,
    val examId: Int,
    val studentId: Int,
    val totalScore: Double?,
    val correctCount: Int?,
    val wrongCount: Int?,
    val useTime: Int?,
    val submitTime: String?
)

data class ResultDetail(
    val result: ExamResult,
    val exam: Exam,
    val answers: List<ResultAnswer>,
    val history: List<ExamHistory>?
)

data class ResultAnswer(
    val questionId: Int,
    val questionContent: String,
    val questionType: String,
    val optionA: String?,
    val optionB: String?,
    val optionC: String?,
    val optionD: String?,
    val studentAnswer: String?,
    val correctAnswer: String?,
    val isCorrect: Boolean?,
    val score: Double?
)

data class ExamHistory(
    val historyId: Int,
    val examId: Int,
    val operatorId: Int?,
    val operatorName: String?,
    val actionType: String,
    val actionDetail: String?,
    val createTime: String?
)
