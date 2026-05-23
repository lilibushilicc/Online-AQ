package com.onlineaq.student.data.model

data class PageResult<T>(
    val list: List<T>,
    val total: Int? = null,
    val page: Int? = null,
    val pageSize: Int? = null,
)

data class UserProfile(
    val userId: Int,
    val username: String,
    val realName: String,
    val role: String,
    val email: String? = null,
    val createTime: String? = null,
)

data class ProfileUpdateRequest(
    val realName: String? = null,
    val email: String? = null,
    val oldPassword: String? = null,
    val newPassword: String? = null,
)

data class WrongNotebook(
    val notebookId: Int,
    val notebookName: String,
    val description: String? = null,
    val createTime: String? = null,
    val itemCount: Int = 0,
)

data class NotebookRequest(
    val notebookName: String,
    val description: String = "",
)

data class NotebookDetail(
    val notebook: WrongNotebook,
    val groups: List<WrongQuestionGroup>,
    val count: Int = 0,
)

data class WrongQuestionGroup(
    val examId: Int,
    val examName: String,
    val questions: List<WrongQuestionItem>,
)

data class WrongQuestionItem(
    val answerId: Int,
    val questionId: Int,
    val questionContent: String,
    val questionType: String,
    val optionA: String? = null,
    val optionB: String? = null,
    val optionC: String? = null,
    val optionD: String? = null,
    val studentAnswer: String? = null,
    val correctAnswer: String? = null,
    val score: Double? = null,
    val submitTime: String? = null,
)

data class AddNotebookItemRequest(
    val answerId: Int,
)

data class FeedbackCreateRequest(
    val questionId: Int,
    val examId: Int? = null,
    val feedbackType: String,
    val description: String,
)

data class QuestionFeedback(
    val feedbackId: Int,
    val questionId: Int,
    val studentId: Int,
    val examId: Int? = null,
    val feedbackType: String,
    val description: String,
    val status: String,
    val createTime: String? = null,
)

data class AnnouncementItem(
    val announcementId: Int,
    val title: String,
    val content: String,
    val createTime: String,
    val read: Boolean,
)

data class UnreadInfo(
    val unreadCount: Int,
    val announcements: List<AnnouncementItem>,
)
