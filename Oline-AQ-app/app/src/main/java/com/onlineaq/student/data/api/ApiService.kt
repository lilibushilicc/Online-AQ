package com.onlineaq.student.data.api

import com.onlineaq.student.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<LoginData>>

    @GET("exams/student")
    suspend fun getStudentExams(): Response<ApiResponse<List<Exam>>>

    @GET("exams/{examId}")
    suspend fun getExamDetail(@Path("examId") examId: Int): Response<ApiResponse<ExamDetail>>

    @POST("exams/{examId}/submit")
    suspend fun submitExam(
        @Path("examId") examId: Int,
        @Body request: SubmitExamRequest
    ): Response<ApiResponse<SubmitResult>>

    @GET("results/my")
    suspend fun getMyResults(): Response<ApiResponse<List<ExamResult>>>

    @GET("results/{resultId}")
    suspend fun getResultDetail(@Path("resultId") resultId: Int): Response<ApiResponse<ResultDetail>>

    @GET("questions")
    suspend fun getQuestions(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 200,
        @Query("category") category: String? = null,
    ): Response<ApiResponse<PageResult<Question>>>

    @GET("questions/categories")
    suspend fun getQuestionCategories(): Response<ApiResponse<List<String>>>

    @GET("users/me")
    suspend fun getMyProfile(): Response<ApiResponse<UserProfile>>

    @PUT("users/me")
    suspend fun updateMyProfile(@Body request: ProfileUpdateRequest): Response<ApiResponse<UserProfile>>

    @GET("announcements/unread")
    suspend fun getUnreadAnnouncements(): Response<ApiResponse<UnreadInfo>>

    @POST("announcements/{id}/read")
    suspend fun markAnnouncementRead(@Path("id") id: Int): Response<ApiResponse<Unit>>

    @POST("announcements/read-all")
    suspend fun markAllAnnouncementsRead(): Response<ApiResponse<Unit>>

    @GET("wrong-notebooks")
    suspend fun getWrongNotebooks(): Response<ApiResponse<List<WrongNotebook>>>

    @POST("wrong-notebooks")
    suspend fun createWrongNotebook(@Body request: NotebookRequest): Response<ApiResponse<WrongNotebook>>

    @PUT("wrong-notebooks/{notebookId}")
    suspend fun updateWrongNotebook(
        @Path("notebookId") notebookId: Int,
        @Body request: NotebookRequest,
    ): Response<ApiResponse<WrongNotebook>>

    @DELETE("wrong-notebooks/{notebookId}")
    suspend fun deleteWrongNotebook(@Path("notebookId") notebookId: Int): Response<ApiResponse<Unit>>

    @GET("wrong-notebooks/{notebookId}")
    suspend fun getWrongNotebookDetail(@Path("notebookId") notebookId: Int): Response<ApiResponse<NotebookDetail>>

    @POST("wrong-notebooks/{notebookId}/items")
    suspend fun addWrongNotebookItem(
        @Path("notebookId") notebookId: Int,
        @Body request: AddNotebookItemRequest,
    ): Response<ApiResponse<Unit>>

    @DELETE("wrong-notebooks/{notebookId}/items/{itemId}")
    suspend fun removeWrongNotebookItem(
        @Path("notebookId") notebookId: Int,
        @Path("itemId") itemId: Int,
    ): Response<ApiResponse<Unit>>

    @GET("results/wrong-questions")
    suspend fun getWrongQuestions(): Response<ApiResponse<List<WrongQuestionGroup>>>

    @GET("feedbacks/my")
    suspend fun getMyFeedbackQuestionIds(@Query("questionIds") questionIds: String): Response<ApiResponse<List<Int>>>

    @POST("feedbacks")
    suspend fun submitFeedback(@Body request: FeedbackCreateRequest): Response<ApiResponse<QuestionFeedback>>
}
