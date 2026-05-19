import { defineStore } from 'pinia'
import type { Role, User, Question, Exam, ExamResult, ExamDetail, ResultDetail, PublishExamPayload, CreateUserPayload, UpdateUserPayload, FeedbackCreatePayload, FeedbackListVO } from '@/types'
import * as api from '@/api'

export type {
  Role, User, Question, Exam, ExamResult,
  ExamDetail, ExamHistory, ExamResult as ExamResultDetail,
  ResultDetail, WrongQuestionGroup, WrongQuestionItem,
  WrongNotebook, NotebookDetail, UploadFileItem,
  QuestionFeedback, FeedbackListVO, FeedbackCreatePayload,
  QuestionScoreItem, CreateExamPayload, PublishExamPayload,
  CreateUserPayload, UpdateUserPayload, BatchCategoryPayload,
} from '@/types'

const TK = 'token', UK = 'user'

function loadUser(): User | null {
  try { return JSON.parse(localStorage.getItem(UK)!) } catch { localStorage.removeItem(UK); return null }
}

function canAccess(e: Exam) {
  const n = Date.now(), s = e.startTime ? new Date(e.startTime).getTime() : null, en = e.endTime ? new Date(e.endTime).getTime() : null
  return e.status === 'published' && !(s !== null && n < s) && !(en !== null && n > en)
}

export const useExamStore = defineStore('exam', {
  state: () => ({
    currentUser: loadUser(),
    questions: [] as Question[],
    exams: [] as Exam[],
    results: [] as ExamResult[],
    users: [] as User[],
    categories: [] as string[],
    latestParsedCount: 0,
  }),
  getters: {
    publishedExams: (s) => s.exams.filter((e) => e.status === 'published'),
    availableExams: (s) => s.exams.filter((e) => canAccess(e)),
    averageScore: (s) => s.results.length ? Math.round(s.results.reduce((a, r) => a + Number(r.totalScore), 0) / s.results.length * 10) / 10 : 0,
  },
  actions: {
    async login(username: string, password: string, role: Role) {
      const d = await api.loginApi(username, password, role)
      localStorage.setItem(TK, d.token)
      const u: User = { userId: d.userId, username: d.username, realName: d.realName, role: d.role }
      localStorage.setItem(UK, JSON.stringify(u)); this.currentUser = u; return u
    },
    logout() { this.currentUser = null; localStorage.removeItem(TK); localStorage.removeItem(UK) },

    // Questions
    async loadQuestions(category?: string) { this.questions = (await api.loadQuestionsApi(category)).list },
    async loadCategories() { this.categories = await api.loadCategoriesApi() },
    async loadUploadFiles() { return api.loadUploadFilesApi() },
    async uploadAndParse(file: File, category?: string) { this.latestParsedCount = await api.uploadAndParseApi(file, category); await this.loadQuestions(); return this.latestParsedCount },
    async deleteQuestion(id: number) { await api.deleteQuestionApi(id); await this.loadQuestions() },
    async deleteQuestions(ids: number[]) { await api.deleteQuestionsApi(ids); await this.loadQuestions() },
    async updateQuestionScores(ids: number[], s: number) { await api.updateQuestionScoresApi(ids, s); await this.loadQuestions() },
    async updateQuestionCategories(ids: number[], category: string) { await api.updateQuestionCategoriesApi(ids, category); await this.loadQuestions(); await this.loadCategories() },

    // Exams
    async loadExams() { this.exams = await api.loadExamsApi() },
    async loadStudentExams() { this.exams = await api.loadStudentExamsApi() },
    getExamDetail: (id: number) => api.getExamDetailApi(id),
    loadExamHistory: (id: number) => api.loadExamHistoryApi(id),
    async createExam(p: Parameters<typeof api.createExamApi>[0]) { await api.createExamApi(p); await this.loadExams() },
    async publishExam(id: number, p?: PublishExamPayload) { await api.publishExamApi(id, p); await this.loadExams() },
    async closeExam(id: number) { await api.closeExamApi(id); await this.loadExams() },
    async deleteExam(id: number) { await api.deleteExamApi(id); await this.loadExams() },
    async submitExam(examId: number, answers: Record<number, string>, useTime = 0) {
      const d = await api.submitExamApi(examId, this.currentUser?.userId, answers, useTime)
      await this.loadMyResults(); return d
    },

    // Results
    async loadMyResults() { this.results = await api.loadMyResultsApi() },
    async loadExamResults(id: number) { this.results = await api.loadExamResultsApi(id) },
    getResultDetail: (id: number) => api.getResultDetailApi(id),
    getWrongQuestions: () => api.getWrongQuestionsApi(),

    // Users
    async loadUsers() { this.users = await api.loadUsersApi() },
    async createUser(p: CreateUserPayload) { await api.createUserApi(p); await this.loadUsers() },
    async updateUser(id: number, p: UpdateUserPayload) { await api.updateUserApi(id, p); await this.loadUsers() },
    async deleteUser(id: number) { await api.deleteUserApi(id); await this.loadUsers() },

    // Config
    loadConfig: () => api.loadConfigApi(),
    saveConfig: (c: Record<string, string>) => api.saveConfigApi(c),
    testR2: () => api.testR2Api(),

    // Notebooks
    loadNotebooks: () => api.loadNotebooksApi(),
    createNotebook: (n: string, d?: string) => api.createNotebookApi(n, d),
    updateNotebook: (id: number, n?: string, d?: string) => api.updateNotebookApi(id, n, d),
    deleteNotebook: (id: number) => api.deleteNotebookApi(id),
    getNotebookDetail: (id: number) => api.getNotebookDetailApi(id),
    addToNotebook: (id: number, aid: number) => api.addToNotebookApi(id, aid),
    removeNotebookItem: (id: number, iid: number) => api.removeNotebookItemApi(id, iid),

    // Feedback
    submitFeedback: (p: FeedbackCreatePayload) => api.submitFeedbackApi(p),
    myFeedbackQuestionIds: (ids: number[]) => api.myFeedbackQuestionIdsApi(ids),
    loadFeedbacks: (s?: string) => api.loadFeedbacksApi(s),
    getFeedbackDetail: (id: number) => api.getFeedbackDetailApi(id),
    resolveFeedback: (id: number, req: Record<string, unknown>) => api.resolveFeedbackApi(id, req),
    rejectFeedback: (id: number, reason: string) => api.rejectFeedbackApi(id, reason),

    // Helpers
    hasSubmittedExam(id: number) { return this.results.some((r) => r.examId === id) },
    isExamAccessible: (e: Exam) => canAccess(e),
  },
})