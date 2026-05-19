export type Role = 'admin' | 'student'

export interface User {
  userId: number
  username: string
  realName: string
  role: Role
}

export type QuestionType = 'single' | 'judge' | 'short_answer' | 'fill_blank'

export interface Question {
  questionId: number
  questionContent: string
  questionType: QuestionType
  optionA: string
  optionB: string
  optionC: string
  optionD: string
  correctAnswer: string
  score: number
  category?: string
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
  assignAll?: boolean
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
  questionType: QuestionType
  optionA: string
  optionB: string
  optionC: string
  optionD: string
  studentAnswer: string
  correctAnswer: string
  isCorrect: boolean
  score: number
}

export interface WrongQuestionItem {
  answerId: number
  questionId: number
  questionContent: string
  questionType: QuestionType
  optionA: string
  optionB: string
  optionC: string
  optionD: string
  studentAnswer: string
  correctAnswer: string
  score: number
  submitTime: string
}

export interface WrongQuestionGroup {
  examId: number
  examName: string
  questions: WrongQuestionItem[]
}

export interface WrongNotebook {
  notebookId: number
  notebookName: string
  description: string
  createTime: string
  itemCount: number
}

export interface NotebookDetail {
  notebook: {
    notebookId: number
    studentId: number
    notebookName: string
    description: string
    createTime: string
  }
  groups: WrongQuestionGroup[]
  count: number
}

export interface ExamDetail {
  exam: Exam
  questions: Question[]
  relationScores?: Record<number, number>
}

export interface ResultDetail {
  result: ExamResult
  exam: Exam
  answers: ResultAnswerDetail[]
  history: ExamHistory[]
}

export interface CreateUserPayload {
  username: string
  password: string
  realName: string
  role: Role
}

export interface UpdateUserPayload {
  realName?: string
  password?: string
}

export interface BatchCategoryPayload {
  questionIds: number[]
  category: string
}

export interface UploadFileItem {
  fileId: number
  fileName: string
  fileType: string
  status: string
  questionCount: number
  createTime: string
}

export interface QuestionFeedback {
  feedbackId: number
  questionId: number
  studentId: number
  examId?: number
  feedbackType: string
  description: string
  status: string
  rejectReason?: string
  resolveType?: string
  createTime: string
  updateTime: string
}

export interface FeedbackListVO {
  feedbackId: number
  questionId: number
  questionContent: string
  studentName: string
  feedbackType: string
  description: string
  status: string
  rejectReason?: string
  createTime: string
  pendingCount?: number
}

export interface FeedbackCreatePayload {
  questionId: number
  examId?: number
  feedbackType: string
  description: string
}

export interface QuestionScoreItem {
  questionId: number
  score: number
}

export interface CreateExamPayload {
  examName: string
  description: string
  duration: number
  startTime?: string | null
  endTime?: string | null
  allowRetake: boolean
  questionIds: number[]
  questionScores?: QuestionScoreItem[]
}

export interface PublishExamPayload {
  assignAll: boolean
  studentIds?: number[]
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}
