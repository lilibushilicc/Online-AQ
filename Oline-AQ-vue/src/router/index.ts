import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useExamStore, type Role } from '@/stores/exam'

type RouteMeta = {
  requiresAuth?: boolean
  role?: Role
  title?: string
  subtitle?: string
  showBack?: boolean
  keepAlive?: boolean
}

const LoginView = () => import('@/views/login/LoginView.vue')
const RegisterView = () => import('@/views/login/RegisterView.vue')
const AdminLayoutWrapper = () => import('@/views/admin/AdminLayoutWrapper.vue')
const StudentLayoutWrapper = () => import('@/views/student/StudentLayoutWrapper.vue')
const AdminDashboard = () => import('@/views/admin/AdminDashboard.vue')
const AdminUpload = () => import('@/views/admin/AdminUpload.vue')
const AdminQuestions = () => import('@/views/admin/AdminQuestions.vue')
const AdminExams = () => import('@/views/admin/AdminExams.vue')
const AdminPapers = () => import('@/views/admin/AdminPapers.vue')
const AdminResults = () => import('@/views/admin/AdminResults.vue')
const AdminReview = () => import('@/views/admin/AdminReview.vue')
const AdminStudents = () => import('@/views/admin/AdminStudents.vue')
const AdminConfig = () => import('@/views/admin/AdminConfig.vue')
const AdminFeedbacks = () => import('@/views/admin/AdminFeedbacks.vue')
const AdminAnnouncements = () => import('@/views/admin/AdminAnnouncements.vue')
const StudentExams = () => import('@/views/student/StudentExams.vue')
const StudentExamDetail = () => import('@/views/student/StudentExamDetail.vue')
const StudentResults = () => import('@/views/student/StudentResults.vue')
const StudentResultDetail = () => import('@/views/student/StudentResultDetail.vue')
const StudentExamHistory = () => import('@/views/student/StudentExamHistory.vue')
const StudentPractice = () => import('@/views/student/StudentPractice.vue')
const PracticeHistory = () => import('@/views/student/PracticeHistory.vue')
const StudentWrongBook = () => import('@/views/student/StudentWrongBook.vue')
const StudentWrongBookDetail = () => import('@/views/student/StudentWrongBookDetail.vue')
const StudentProfile = () => import('@/views/student/StudentProfile.vue')

// Scroll position memory
const scrollPositions = new Map<string, number>()
let isNavigating = false
let navLockTimeout: ReturnType<typeof setTimeout> | null = null

const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/login' },
  { path: '/login', component: LoginView, meta: { title: '登录' } },
  { path: '/register', component: RegisterView, meta: { title: '注册' } },
  {
    path: '/admin',
    component: AdminLayoutWrapper,
    meta: { requiresAuth: true, role: 'admin' },
    children: [
      { path: 'dashboard', component: AdminDashboard, meta: { title: '教学管控制台', subtitle: '全局数据概览与快捷操作面板', keepAlive: true } },
      { path: 'upload', component: AdminUpload, meta: { title: '上传题库', subtitle: '上传 TXT 或 DOCX 文件并指定类别，系统将自动解析题目。', keepAlive: true } },
      { path: 'questions', component: AdminQuestions, meta: { title: '题目管理', subtitle: '查看、编辑、筛选和批量操作题目。', keepAlive: true } },
      { path: 'papers', component: AdminPapers, meta: { title: '试卷管理', subtitle: '创建和配置试卷模板，发布为正式考试。' } },
      { path: 'exams', component: AdminExams, meta: { title: '考试管理', keepAlive: true } },
      { path: 'results', component: AdminResults, meta: { title: '成绩查看', keepAlive: true } },
      { path: 'review', component: AdminReview, meta: { title: '批阅管理', subtitle: '对主观题进行人工批阅。', keepAlive: true } },
      { path: 'students', component: AdminStudents, meta: { title: '学生管理', keepAlive: true } },
      { path: 'config', redirect: '/admin/config/storage' },
      { path: 'config/storage', component: AdminConfig, meta: { title: '系统设置', subtitle: '设置存储格式与 R2 存储服务。' } },
      { path: 'config/ai', component: AdminConfig, meta: { title: '系统设置', subtitle: '配置 AI 接口与智能解析题目。' } },
      { path: 'config/login', component: AdminConfig, meta: { title: '系统设置', subtitle: '配置管理员登录方式、Logo 和点击次数。' } },
      { path: 'config/email', component: AdminConfig, meta: { title: '系统设置', subtitle: '配置 SMTP 服务并开启邮箱注册功能。' } },
      { path: 'config/version', component: AdminConfig, meta: { title: '系统设置', subtitle: '配置 App 版本号和更新信息。' } },
      { path: 'feedbacks', component: AdminFeedbacks, meta: { title: '意见反馈', subtitle: '查看和管理学生提交的问题和建议。', keepAlive: true } },
      { path: 'announcements', component: AdminAnnouncements, meta: { title: '公告管理', subtitle: '创建和管理系统公告，学生登录时收到最新通知。' } },
      { path: 'profile', component: StudentProfile, meta: { title: '个人信息' } },
    ],
  },
  {
    path: '/student',
    component: StudentLayoutWrapper,
    meta: { requiresAuth: true, role: 'student' },
    children: [
      { path: 'exams', component: StudentExams, meta: { title: '考试列表', subtitle: '查看当前可用的考试。', keepAlive: true } },
      { path: 'exams/:examId', component: StudentExamDetail, meta: { title: '考试作答' } },
      { path: 'results', component: StudentResults, meta: { title: '我的成绩', subtitle: '试卷查看和考试记录汇总。', keepAlive: true } },
      { path: 'results/exam/:examId', component: StudentExamHistory, meta: { title: '试卷详情与回顾', showBack: true } },
      { path: 'results/:resultId', component: StudentResultDetail, meta: { title: '成绩详情' } },
      { path: 'practice', component: StudentPractice, meta: { title: '刷题练习', subtitle: '自由练习提升学习成效。', keepAlive: true } },
      { path: 'practice-history', component: PracticeHistory, meta: { title: '练习历史', keepAlive: true } },
      { path: 'wrong-book', component: StudentWrongBook, meta: { title: '错题本', subtitle: '管理和回顾你的错题。', keepAlive: true } },
      { path: 'wrong-book/:notebookId', component: StudentWrongBookDetail, meta: { title: '错题本详情', subtitle: '查看错题本中的题目', showBack: true } },
      { path: 'profile', component: StudentProfile, meta: { title: '个人信息' } },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/login' },
]

function getDefaultRoute(role?: Role) {
  return role === 'admin' ? '/admin/dashboard' : '/student/exams'
}

function getRouteTitle(to: { path: string; meta: Record<string, unknown> }): string {
  const metaTitle = to.meta.title as string | undefined
  if (metaTitle) return metaTitle + ' - Online-AQ Exam System'
  const segments = to.path.split('/').filter(Boolean)
  for (const segment of segments) {
    if (segment !== 'admin' && segment !== 'student') {
      const label = TITLE_MAP[segment] || segment
      return label + ' - Online-AQ Exam System'
    }
  }
  return 'Online-AQ Exam System'
}

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    // Try saved position first
    if (savedPosition) { return { top: savedPosition.top, behavior: 'smooth' as const }
    }
    // Try remembered position
    const pos = scrollPositions.get(to.path)
    if (pos !== undefined) {
      return { top: pos, behavior: 'instant' }
    }
    // Default: scroll to top
    return { top: 0, behavior: 'instant' }
  },
})

router.beforeEach((to, from) => {
  const store = useExamStore()
  const meta = to.meta as RouteMeta

  // Remember scroll position before leaving
  scrollPositions.set(from.path, window.scrollY)

  // Prevent rapid duplicate navigation
  if (isNavigating && to.path === from.path) {
    return false
  }
  if (navLockTimeout) {
    return false
  }

  if (meta.requiresAuth && !store.currentUser) {
    return '/login'
  }

  if (meta.role && store.currentUser?.role !== meta.role) {
    return getDefaultRoute(store.currentUser?.role)
  }

  isNavigating = true
  navLockTimeout = setTimeout(() => {
    isNavigating = false
    navLockTimeout = null
  }, 300)

  return true
})

const TITLE_MAP: Record<string, string> = {
  login: '登录',
  register: '邮箱注册',
  dashboard: '教学管控制台',
  upload: '上传题库',
  questions: '题目管理',
  papers: '试卷管理',
  exams: '考试管理',
  results: '成绩查看',
  review: '批阅管理',
  students: '学生管理',
  config: '系统设置',
  feedbacks: '意见反馈',
  announcements: '公告管理',
  practice: '刷题练习',
  'practice-history': '练习历史',
  'wrong-book': '错题本',
  profile: '个人信息',
}

router.afterEach((to) => {
  document.title = getRouteTitle(to)
})

export default router





