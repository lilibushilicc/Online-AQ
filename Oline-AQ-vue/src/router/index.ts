import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useExamStore, type Role } from '@/stores/exam'

type RouteMeta = {
  requiresAuth?: boolean
  role?: Role
}

const LoginView = () => import('@/views/login/LoginView.vue')
const AdminDashboard = () => import('@/views/admin/AdminDashboard.vue')
const AdminUpload = () => import('@/views/admin/AdminUpload.vue')
const AdminQuestions = () => import('@/views/admin/AdminQuestions.vue')
const AdminExams = () => import('@/views/admin/AdminExams.vue')
const AdminResults = () => import('@/views/admin/AdminResults.vue')
const AdminStudents = () => import('@/views/admin/AdminStudents.vue')
const AdminConfig = () => import('@/views/admin/AdminConfig.vue')
const AdminFeedbacks = () => import('@/views/admin/AdminFeedbacks.vue')
const StudentExams = () => import('@/views/student/StudentExams.vue')
const StudentExamDetail = () => import('@/views/student/StudentExamDetail.vue')
const StudentResults = () => import('@/views/student/StudentResults.vue')
const StudentResultDetail = () => import('@/views/student/StudentResultDetail.vue')
const StudentPractice = () => import('@/views/student/StudentPractice.vue')
const StudentWrongBook = () => import('@/views/student/StudentWrongBook.vue')
const StudentWrongBookDetail = () => import('@/views/student/StudentWrongBookDetail.vue')

const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/login' },
  { path: '/login', component: LoginView },
  { path: '/admin/dashboard', component: AdminDashboard, meta: { requiresAuth: true, role: 'admin' } },
  { path: '/admin/upload', component: AdminUpload, meta: { requiresAuth: true, role: 'admin' } },
  { path: '/admin/questions', component: AdminQuestions, meta: { requiresAuth: true, role: 'admin' } },
  { path: '/admin/exams', component: AdminExams, meta: { requiresAuth: true, role: 'admin' } },
  { path: '/admin/exams/create', component: AdminExams, meta: { requiresAuth: true, role: 'admin' } },
  { path: '/admin/results', component: AdminResults, meta: { requiresAuth: true, role: 'admin' } },
  { path: '/admin/students', component: AdminStudents, meta: { requiresAuth: true, role: 'admin' } },
  { path: '/admin/config', component: AdminConfig, meta: { requiresAuth: true, role: 'admin' } },
  { path: '/admin/feedbacks', component: AdminFeedbacks, meta: { requiresAuth: true, role: 'admin' } },
  { path: '/student/exams', component: StudentExams, meta: { requiresAuth: true, role: 'student' } },
  { path: '/student/exams/:examId', component: StudentExamDetail, meta: { requiresAuth: true, role: 'student' } },
  { path: '/student/results', component: StudentResults, meta: { requiresAuth: true, role: 'student' } },
  { path: '/student/results/:resultId', component: StudentResultDetail, meta: { requiresAuth: true, role: 'student' } },
  { path: '/student/practice', component: StudentPractice, meta: { requiresAuth: true, role: 'student' } },
  { path: '/student/wrong-book', component: StudentWrongBook, meta: { requiresAuth: true, role: 'student' } },
  { path: '/student/wrong-book/:notebookId', component: StudentWrongBookDetail, meta: { requiresAuth: true, role: 'student' } },
  { path: '/:pathMatch(.*)*', redirect: '/login' },
]

function getDefaultRoute(role?: Role) {
  return role === 'admin' ? '/admin/dashboard' : '/student/exams'
}

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0, behavior: 'instant' as const }
  },
})

router.beforeEach((to) => {
  const store = useExamStore()
  const meta = to.meta as RouteMeta

  if (meta.requiresAuth && !store.currentUser) {
    return '/login'
  }

  if (meta.role && store.currentUser?.role !== meta.role) {
    return getDefaultRoute(store.currentUser?.role)
  }

  return true
})

const TITLE_MAP: Record<string, string> = {
  login: '登录',
  dashboard: '教学控制台',
  upload: '上传试题',
  questions: '题库管理',
  exams: '考试管理',
  results: '成绩查看',
  students: '学生管理',
  config: '系统配置',
  feedbacks: '反馈管理',
  practice: '在线做题',
  'wrong-book': '错题本',
}

router.afterEach((to) => {
  const segments = to.path.split('/').filter(Boolean)
  let title = '智能在线答题系统'
  for (const segment of segments) {
    if (segment !== 'admin' && segment !== 'student') {
      const label = TITLE_MAP[segment] || segment
      title = `${label} - 智能在线答题系统`
      break
    }
  }
  document.title = title
})

export default router
