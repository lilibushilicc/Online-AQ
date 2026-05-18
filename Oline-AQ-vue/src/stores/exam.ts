import { defineStore } from 'pinia'
import request from '@/utils/request'

export type Role = 'admin' | 'student'

const TOKEN_STORAGE_KEY = 'token'
const USER_STORAGE_KEY = 'user'

export interface User {
  userId: number
  username: string
  realName: string
  role: Role
}

export interface Question {
  questionId: number
  questionContent: string
  questionType: 'single' | 'judge'
  optionA: string
  optionB: string
  optionC: string
  optionD: string
  correctAnswer: string
  score: number
  sourceFileId?: number
}

export interface Exam {
  examId: number
  examName: string
  description: string
  duration: number
  status: 'draft' | 'published' | 'closed'
  totalScore: number
  allowRetake: boolean
  startTime?: string | null
  endTime?: string | null
  createTime?: string
}

export interface ExamResult {
  resultId: number
  examId: number
  studentId: number
  totalScore: number
  correctCount: number
  wrongCount: number
  useTime: number
  submitTime: string
}

export interface ExamHistory {
  historyId: number
  examId: number
  operatorId: number | null
  operatorName?: string
  actionType: string
  actionDetail: string
  createTime: string
}

export interface ResultAnswerDetail {
  questionId: number
  questionContent: string
  questionType: 'single' | 'judge'
  optionA: string
  optionB: string
  optionC: string
  optionD: string
  studentAnswer: string
  correctAnswer: string
  isCorrect: boolean
  score: number
}

export interface ExamDetail {
  exam: Exam
  questions: Question[]
}

export interface ResultDetail {
  result: ExamResult
  exam: Exam
  answers: ResultAnswerDetail[]
  history: ExamHistory[]
}

export interface CreateExamPayload {
  examName: string
  description: string
  duration: number
  startTime?: string | null
  endTime?: string | null
  allowRetake: boolean
  questionIds: number[]
}

interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

function readCurrentUser() {
  const rawUser = localStorage.getItem(USER_STORAGE_KEY)
  if (!rawUser) {
    return null
  }

  try {
    return JSON.parse(rawUser) as User
  } catch {
    localStorage.removeItem(USER_STORAGE_KEY)
    return null
  }
}

function persistCurrentUser(user: User) {
  localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user))
}

function toTimeValue(value?: string | null) {
  if (!value) {
    return null
  }

  const time = new Date(value).getTime()
  return Number.isNaN(time) ? null : time
}

function isExamPublished(exam: Exam) {
  return exam.status === 'published'
}

function isExamInTimeWindow(exam: Exam) {
  const now = Date.now()
  const startTime = toTimeValue(exam.startTime)
  const endTime = toTimeValue(exam.endTime)

  if (startTime !== null && now < startTime) {
    return false
  }
  if (endTime !== null && now > endTime) {
    return false
  }
  return true
}

export const useExamStore = defineStore('exam', {
  state: () => ({
    currentUser: readCurrentUser(),
    questions: [] as Question[],
    exams: [] as Exam[],
    results: [] as ExamResult[],
    latestParsedCount: 0,
  }),
  getters: {
    publishedExams: (state) => state.exams.filter((exam) => isExamPublished(exam)),
    availableExams: (state) => state.exams.filter((exam) => isExamPublished(exam) && isExamInTimeWindow(exam)),
    averageScore: (state) => {
      if (state.results.length === 0) return 0
      const total = state.results.reduce((sum, result) => sum + Number(result.totalScore), 0)
      return Math.round((total / state.results.length) * 10) / 10
    },
  },
  actions: {
    async login(username: string, password: string, role: Role) {
      const { data } = await request.post<unknown, ApiResponse<User & { token: string }>>('/auth/login', {
        username,
        password,
        role,
      })

      localStorage.setItem(TOKEN_STORAGE_KEY, data.token)
      const user = {
        userId: data.userId,
        username: data.username,
        realName: data.realName,
        role: data.role,
      }
      persistCurrentUser(user)
      this.currentUser = user
      return user
    },
    logout() {
      this.currentUser = null
      localStorage.removeItem(TOKEN_STORAGE_KEY)
      localStorage.removeItem(USER_STORAGE_KEY)
    },
    async loadQuestions() {
      const { data } = await request.get<unknown, ApiResponse<Question[]>>('/questions')
      this.questions = data
    },
    async uploadAndParse(file: File) {
      const formData = new FormData()
      formData.append('file', file)
      const { data: uploadData } = await request.post<unknown, ApiResponse<{ fileId: number }>>('/files/upload', formData)
      const { data: parsedData } = await request.post<unknown, ApiResponse<{ questionCount: number }>>(
        `/files/${uploadData.fileId}/parse`,
      )
      this.latestParsedCount = parsedData.questionCount
      await this.loadQuestions()
      return parsedData.questionCount
    },
    async deleteQuestion(questionId: number) {
      await request.delete(`/questions/${questionId}`)
      await this.loadQuestions()
    },
    async deleteQuestions(questionIds: number[]) {
      await request.post('/questions/batch-delete', { questionIds })
      await this.loadQuestions()
    },
    async updateQuestionScores(questionIds: number[], score: number) {
      await request.post('/questions/batch-score', { questionIds, score })
      await this.loadQuestions()
    },
    async loadExams() {
      const { data } = await request.get<unknown, ApiResponse<Exam[]>>('/exams')
      this.exams = data
    },
    async getExamDetail(examId: number) {
      const { data } = await request.get<unknown, ApiResponse<ExamDetail>>(`/exams/${examId}`)
      return data
    },
    async loadExamHistory(examId: number) {
      const { data } = await request.get<unknown, ApiResponse<ExamHistory[]>>(`/exams/${examId}/history`)
      return data
    },
    async createExam(payload: CreateExamPayload) {
      await request.post('/exams', payload)
      await this.loadExams()
    },
    async publishExam(examId: number) {
      await request.put(`/exams/${examId}/publish`)
      await this.loadExams()
    },
    async closeExam(examId: number) {
      await request.put(`/exams/${examId}/close`)
      await this.loadExams()
    },
    async submitExam(examId: number, answers: Record<number, string>, useTime = 0) {
      const payload = {
        studentId: this.currentUser?.userId,
        useTime,
        answers: Object.entries(answers).map(([questionId, studentAnswer]) => ({
          questionId: Number(questionId),
          studentAnswer,
        })),
      }
      const { data } = await request.post<
        unknown,
        ApiResponse<{ resultId: number; totalScore: number; correctCount: number; wrongCount: number }>
      >(`/exams/${examId}/submit`, payload)
      await this.loadMyResults()
      return data
    },
    async loadMyResults() {
      const { data } = await request.get<unknown, ApiResponse<ExamResult[]>>('/results/my')
      this.results = data
    },
    async loadExamResults(examId: number) {
      const { data } = await request.get<unknown, ApiResponse<ExamResult[]>>(`/results/exam/${examId}`)
      this.results = data
    },
    async getResultDetail(resultId: number) {
      const { data } = await request.get<unknown, ApiResponse<ResultDetail>>(`/results/${resultId}`)
      return data
    },
    getExamResultHistory(examId: number) {
      return this.results.filter((result) => result.examId === examId)
    },
    hasSubmittedExam(examId: number) {
      return this.results.some((result) => result.examId === examId)
    },
    isExamAccessible(exam: Exam) {
      return isExamPublished(exam) && isExamInTimeWindow(exam)
    },
  },
})
