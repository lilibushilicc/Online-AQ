import { apiGet, apiPost, apiPut, apiDelete } from '@/utils/apiHelper'
import type {
  User,
  Question,
  Exam,
  ExamDetail,
  ExamHistory,
  ExamResult,
  ResultDetail,
  WrongQuestionGroup,
  WrongNotebook,
  NotebookDetail,
  UploadFileItem,
  FeedbackListVO,
  QuestionFeedback,
  FeedbackCreatePayload,
  CreateExamPayload,
  PublishExamPayload,
  CreateUserPayload,
  UpdateUserPayload,
  PageResult,
} from '@/types'

// -------- Auth --------
export function loginApi(username: string, password: string, role: string) {
  return apiPost<User & { token: string }>('/auth/login', { username, password, role })
}

// -------- Questions --------
export function loadQuestionsApi(category?: string, page = 1, pageSize = 200) {
  const params: Record<string, string | number> = { page, pageSize }
  if (category) params.category = category
  return apiGet<PageResult<Question>>('/questions', params)
}

export function loadCategoriesApi() {
  return apiGet<string[]>('/questions/categories')
}

export function deleteQuestionApi(questionId: number) {
  return apiDelete(`/questions/${questionId}`)
}

export function deleteQuestionsApi(questionIds: number[]) {
  return apiPost('/questions/batch-delete', { questionIds })
}

export function updateQuestionScoresApi(questionIds: number[], score: number) {
  return apiPost('/questions/batch-score', { questionIds, score })
}

export function updateQuestionCategoriesApi(questionIds: number[], category: string) {
  return apiPost('/questions/batch-category', { questionIds, category })
}

// -------- Files --------
export function loadUploadFilesApi() {
  return apiGet<UploadFileItem[]>('/files')
}

export function deleteFileApi(fileId: number) {
  return apiDelete(`/files/${fileId}`)
}

export function uploadAndParseApi(file: File, category?: string, useAi = false) {
  const formData = new FormData()
  formData.append('file', file)
  return apiPost<{ fileId: number }>('/files/upload', formData).then((uploadData) =>
    apiPost<{ questionCount: number }>(`/files/${uploadData.fileId}/parse`, { category, useAi })
      .then((parsedData) => parsedData.questionCount),
  )
}

// -------- Exams --------
export function loadExamsApi() {
  return apiGet<Exam[]>('/exams')
}

export function loadStudentExamsApi() {
  return apiGet<Exam[]>('/exams/student')
}

export function getExamDetailApi(examId: number) {
  return apiGet<ExamDetail>(`/exams/${examId}`)
}

export function loadExamHistoryApi(examId: number) {
  return apiGet<ExamHistory[]>(`/exams/${examId}/history`)
}

export function createExamApi(payload: CreateExamPayload) {
  return apiPost('/exams', payload)
}

export function publishExamApi(examId: number, payload?: PublishExamPayload) {
  return apiPut(`/exams/${examId}/publish`, payload ?? {})
}

export function closeExamApi(examId: number) {
  return apiPut(`/exams/${examId}/close`)
}

export function deleteExamApi(examId: number) {
  return apiDelete(`/exams/${examId}`)
}

export function submitExamApi(examId: number, studentId: number | undefined, answers: Record<number, string>, useTime = 0) {
  return apiPost<{ resultId: number; totalScore: number; correctCount: number; wrongCount: number }>(
    `/exams/${examId}/submit`,
    {
      studentId,
      useTime,
      answers: Object.entries(answers).map(([questionId, studentAnswer]) => ({
        questionId: Number(questionId),
        studentAnswer,
      })),
    },
  )
}

// -------- Results --------
export function loadMyResultsApi() {
  return apiGet<ExamResult[]>('/results/my')
}

export function loadExamResultsApi(examId: number) {
  return apiGet<ExamResult[]>(`/results/exam/${examId}`)
}

export function getResultDetailApi(resultId: number) {
  return apiGet<ResultDetail>(`/results/${resultId}`)
}

export function getWrongQuestionsApi() {
  return apiGet<WrongQuestionGroup[]>('/results/wrong-questions')
}

// -------- Users --------
export function loadUsersApi() {
  return apiGet<User[]>('/users')
}

export function createUserApi(payload: CreateUserPayload) {
  return apiPost('/users', payload)
}

export function updateUserApi(userId: number, payload: UpdateUserPayload) {
  return apiPut(`/users/${userId}`, payload)
}

export function deleteUserApi(userId: number) {
  return apiDelete(`/users/${userId}`)
}

// -------- Config --------
export function loadConfigApi() {
  return apiGet<Record<string, string>>('/config')
}

export function saveConfigApi(config: Record<string, string>) {
  return apiPut('/config', config)
}

export function testR2Api() {
  return apiPost<{ result: string }>('/config/test-r2')
}

export function testAiApi() {
  return apiPost<{ result: string }>('/config/test-ai')
}

// -------- Wrong Notebooks --------
export function loadNotebooksApi() {
  return apiGet<WrongNotebook[]>('/wrong-notebooks')
}

export function createNotebookApi(notebookName: string, description?: string) {
  return apiPost<WrongNotebook>('/wrong-notebooks', { notebookName, description: description || '' })
}

export function updateNotebookApi(notebookId: number, notebookName?: string, description?: string) {
  return apiPut<WrongNotebook>(`/wrong-notebooks/${notebookId}`, { notebookName, description })
}

export function deleteNotebookApi(notebookId: number) {
  return apiDelete(`/wrong-notebooks/${notebookId}`)
}

export function getNotebookDetailApi(notebookId: number) {
  return apiGet<NotebookDetail>(`/wrong-notebooks/${notebookId}`)
}

export function addToNotebookApi(notebookId: number, answerId: number) {
  return apiPost<{ id: number; added: boolean }>(`/wrong-notebooks/${notebookId}/items`, { answerId })
}

export function removeNotebookItemApi(notebookId: number, itemId: number) {
  return apiDelete(`/wrong-notebooks/${notebookId}/items/${itemId}`)
}

// -------- Feedback --------
export function submitFeedbackApi(payload: FeedbackCreatePayload) {
  return apiPost<QuestionFeedback>('/feedbacks', payload)
}

export function myFeedbackQuestionIdsApi(questionIds: number[]) {
  return apiGet<number[]>('/feedbacks/my', { questionIds: questionIds.join(',') })
}

export function loadFeedbacksApi(status?: string) {
  return apiGet<FeedbackListVO[]>('/feedbacks', status ? { status } : undefined)
}

export function getFeedbackDetailApi(feedbackId: number) {
  return apiGet<any>(`/feedbacks/${feedbackId}`)
}

export function resolveFeedbackApi(feedbackId: number, questionRequest: Record<string, unknown>) {
  return apiPut<{ affectedOther: number }>(`/feedbacks/${feedbackId}/resolve`, questionRequest)
}

export function rejectFeedbackApi(feedbackId: number, rejectReason: string) {
  return apiPut<{ affectedOther: number }>(`/feedbacks/${feedbackId}/reject`, { rejectReason })
}
