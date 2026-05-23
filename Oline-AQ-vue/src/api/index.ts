import { apiGet, apiPost, apiPut, apiDelete } from '@/utils/apiHelper'
import type {
  User,
  Question,
  Exam,
  ExamDetail,
  ExamHistory,
  ExamResult,
  ResultDetail,
  ReviewItem,
  PracticeResult,
  SubmitPracticePayload,
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
  Announcement,
  UnreadInfo,
  PageResult,
} from '@/types'

// -------- Auth --------
export function loginApi(username: string, password: string, role: string) {
  return apiPost<User & { token: string }>('/auth/login', { username, password, role })
}

export function sendCodeApi(email: string) {
  return apiPost<void>('/auth/register/send-code', { email })
}

export function registerApi(email: string, code: string, password: string, realName: string) {
  return apiPost<User & { token: string }>('/auth/register', { email, code, password, realName })
}

// -------- Questions --------
export function loadQuestionsApi(category?: string, page = 1, pageSize = 1000) {
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

export function updateQuestionApi(questionId: number, data: {
  questionContent: string; questionType: string; correctAnswer: string; score: number;
  optionA?: string; optionB?: string; optionC?: string; optionD?: string; category?: string;
}) {
  return apiPut<Question>(`/questions/${questionId}`, data)
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

export function uploadAndParseApi(file: File, category?: string, useAi = false, fileName?: string) {
  const formData = new FormData()
  formData.append('file', file)
  if (fileName) formData.append('name', fileName)
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

export function getExamDetailApi(examId: number, attemptId?: string) {
  return apiGet<ExamDetail>(`/exams/${examId}`, attemptId ? { attemptId } : undefined)
}

export function loadExamHistoryApi(examId: number) {
  return apiGet<ExamHistory[]>(`/exams/${examId}/history`)
}

export function createExamApi(payload: CreateExamPayload) {
  return apiPost('/exams', payload)
}

export function updateExamApi(examId: number, payload: CreateExamPayload) {
  return apiPut(`/exams/${examId}`, payload)
}

export function publishExamApi(examId: number, payload?: PublishExamPayload) {
  return apiPut(`/exams/${examId}/publish`, payload ?? {})
}

export function updateExamSettingsApi(examId: number, payload: { endTime?: string | null; allowRetake?: boolean }) {
  return apiPut(`/exams/${examId}/settings`, payload)
}

export function closeExamApi(examId: number) {
  return apiPut(`/exams/${examId}/close`)
}

export function deleteExamApi(examId: number) {
  return apiDelete(`/exams/${examId}`)
}

export function getDashboardStatsApi() {
  return apiGet<{
    scoreDistribution: { range0_20: number; range21_40: number; range41_60: number; range61_80: number; range81_100: number }
    passRate: number
    hardestQuestions: { questionId: number; questionContent: string; correctRate: number; totalAnswers: number }[]
  }>('/dashboard/stats')
}

export function saveDraftApi(examId: number, attemptId: string | undefined, answers: { questionId: number; studentAnswer: string }[], useTime: number) {
  return apiPut<void>(`/exams/${examId}/draft`, { attemptId, answers, useTime })
}

export function loadDraftApi(examId: number) {
  return apiGet<{ id: number; examId: number; studentId: number; attemptId?: string; answers: string; shuffleSnapshot?: string; useTime: number; updatedAt: string } | null>(`/exams/${examId}/draft`)
}

export function clearDraftApi(examId: number) {
  return apiDelete<void>(`/exams/${examId}/draft`)
}

export function submitExamApi(examId: number, attemptId: string | undefined, answers: Record<number, string>, useTime = 0) {
  return apiPost<{ resultId: number; totalScore: number; correctCount: number; wrongCount: number }>(
    `/exams/${examId}/submit`,
    {
      attemptId,
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

export function getPendingReviewsApi() {
  return apiGet<ReviewItem[]>('/results/pending-review')
}

export function reviewAnswerApi(answerId: number, score: number) {
  return apiPut<void>(`/results/review/${answerId}?score=${score}`)
}

export function submitPracticeApi(payload: SubmitPracticePayload) {
  return apiPost<PracticeResult>('/practice/submit', payload)
}

export function getPracticeHistoryApi() {
  return apiGet<PracticeResult[]>('/practice/history')
}

// -------- Users --------
export function getUserApi(userId: number) {
  return apiGet<User>(`/users/${userId}`)
}

export function loadUsersApi() {
  return apiGet<User[]>('/users')
}

export function getMyProfileApi() {
  return apiGet<User>('/users/me')
}

export function updateMyProfileApi(payload: { realName?: string; email?: string; oldPassword?: string; newPassword?: string }) {
  return apiPut<User>('/users/me', payload)
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

export function loadPublicLoginConfigApi() {
  return apiGet<Record<string, string>>('/config/public/login')
}

export function testR2Api() {
  return apiPost<{ result: string }>('/config/test-r2')
}

export function testAiApi() {
  return apiPost<{ result: string }>('/config/test-ai')
}

export function testEmailApi(payload?: { accountId?: number }) {
  return apiPost<{ result: string }>('/config/test-email', payload ?? {})
}

export function loadEmailStatsApi() {
  return apiGet<{ todayCount: number; weekCount: number; totalCount: number; lastSendTime: string | null }>('/config/email-stats')
}

export interface SmtpAccountData {
  id?: number
  host: string
  port: number
  username: string
  password: string
  fromAddress: string
  active?: boolean
  enabled?: boolean
  hourlyLimit?: number
  dailyLimit?: number
  hourlySent?: number
  dailySent?: number
  createTime?: string
}

export function loadSmtpAccountsApi() {
  return apiGet<SmtpAccountData[]>('/config/smtp-accounts')
}

export function createSmtpAccountApi(account: SmtpAccountData) {
  return apiPost('/config/smtp-accounts', account)
}

export function updateSmtpAccountApi(id: number, account: Partial<SmtpAccountData>) {
  return apiPut(`/config/smtp-accounts/${id}`, account)
}

export function deleteSmtpAccountApi(id: number) {
  return apiDelete(`/config/smtp-accounts/${id}`)
}

export function activateSmtpAccountApi(id: number) {
  return apiPut(`/config/smtp-accounts/${id}/activate`)
}

export function testSmtpInlineApi(account: Partial<SmtpAccountData>) {
  return apiPost<{ result: string }>('/config/smtp-accounts/test', account)
}

export function toggleSmtpEnabledApi(id: number) {
  return apiPut(`/config/smtp-accounts/${id}/toggle-enabled`)
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

// -------- Announcements --------
export function loadAnnouncementsApi() {
  return apiGet<Announcement[]>('/announcements')
}

export function getAnnouncementApi(id: number) {
  return apiGet<Announcement>(`/announcements/${id}`)
}

export function createAnnouncementApi(payload: { title: string; content: string; active?: boolean }) {
  return apiPost<Announcement>('/announcements', payload)
}

export function updateAnnouncementApi(id: number, payload: { title: string; content: string; active?: boolean }) {
  return apiPut<Announcement>(`/announcements/${id}`, payload)
}

export function deleteAnnouncementApi(id: number) {
  return apiDelete(`/announcements/${id}`)
}

export function getUnreadAnnouncementsApi() {
  return apiGet<UnreadInfo>('/announcements/unread')
}

export function markAnnouncementReadApi(id: number) {
  return apiPost(`/announcements/${id}/read`)
}

export function markAllAnnouncementsReadApi() {
  return apiPost('/announcements/read-all')
}
