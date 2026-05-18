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
}
