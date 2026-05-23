import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useExamStore, type Role } from '@/stores/exam'

type RouteMeta = {
  requiresAuth?: boolean
  role?: Role
  title?: string
  subtitle?: string
  showBack?: boolean
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

const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/login' },
  { path: '/login', component: LoginView },
  { path: '/register', component: RegisterView },
  {
    path: '/admin',
    component: AdminLayoutWrapper,
    meta: { requiresAuth: true, role: 'admin' },
    children: [
      { path: 'dashboard', component: AdminDashboard, meta: { title: '教学控制台', subtitle: '考试数据概览与快捷操作。' } },
      { path: 'upload', component: AdminUpload, meta: { title: '上传试题', subtitle: '上传 TXT 或 DOCX 文件，可指定分类，后端自动解析并保存到题库。' } },
      { path: 'questions', component: AdminQuestions, meta: { title: '题库管理', subtitle: '按文件、分类、题型筛选题目，并进行批量操作。' } },
      { path: 'papers', component: AdminPapers, meta: { title: '试卷管理', subtitle: '创建和管理试卷草稿，组卷后发布为正式考试。' } },
      { path: 'exams', component: AdminExams, meta: { title: '考试管理' } },
      { path: 'results', component: AdminResults, meta: { title: '成绩查看' } },
      { path: 'review', component: AdminReview, meta: { title: '评分管理', subtitle: '对简答题进行人工评分。' } },
      { path: 'students', component: AdminStudents, meta: { title: '学生管理' } },
      { path: 'config', redirect: '/admin/config/storage' },
      { path: 'config/storage', component: AdminConfig, meta: { title: '系统配置', subtitle: '配置存储方式与 R2 云存储。' } },
      { path: 'config/ai', component: AdminConfig, meta: { title: '系统配置', subtitle: '配置 AI 接口用于智能解析试题。' } },
      { path: 'config/login', component: AdminConfig, meta: { title: '系统配置', subtitle: '配置管理员登录方式与点击 Logo 进入次数。' } },
      { path: 'config/email', component: AdminConfig, meta: { title: '系统配置', subtitle: '配置 SMTP 服务并开启邮箱注册功能。' } },
      { path: 'feedbacks', component: AdminFeedbacks, meta: { title: '反馈管理', subtitle: '查看并处理学生对试题的反馈与申诉。' } },
      { path: 'announcements', component: AdminAnnouncements, meta: { title: '公告管理', subtitle: '发布和管理系统公告，学生登录后将收到弹窗通知。' } },
      { path: 'profile', component: StudentProfile, meta: { title: '个人中心' } },
    ],
  },
  {
    path: '/student',
    component: StudentLayoutWrapper,
    meta: { requiresAuth: true, role: 'student' },
    children: [
      { path: 'exams', component: StudentExams, meta: { title: '考试列表', subtitle: '查看并参与可用的在线考试。' } },
      { path: 'exams/:examId', component: StudentExamDetail, meta: { title: '考试作答' } },
      { path: 'results', component: StudentResults, meta: { title: '我的成绩', subtitle: '按试卷查看历次考试记录与得分情况。' } },
      { path: 'results/exam/:examId', component: StudentExamHistory, meta: { title: '试卷历次作答', showBack: true } },
      { path: 'results/:resultId', component: StudentResultDetail, meta: { title: '成绩详情' } },
      { path: 'practice', component: StudentPractice, meta: { title: '在线做题', subtitle: '随机抽题练习，检验学习成果。' } },
      { path: 'practice-history', component: PracticeHistory, meta: { title: '练习历史' } },
      { path: 'wrong-book', component: StudentWrongBook, meta: { title: '错题本', subtitle: '整理和管理你的错题。' } },
      { path: 'wrong-book/:notebookId', component: StudentWrongBookDetail, meta: { title: '错题本详情', subtitle: '查看错题本中的题目', showBack: true } },
      { path: 'profile', component: StudentProfile, meta: { title: '个人中心' } },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/login' },
]

function getDefaultRoute(role?: Role) {
  return role === 'admin' ? '/admin/dashboard' : '/student/exams'
}

function getRouteTitle(to: { path: string; meta: Record<string, unknown> }): string {
  const metaTitle = to.meta.title as string | undefined
  if (metaTitle) return `${metaTitle} - 智能在线答题系统`
  const segments = to.path.split('/').filter(Boolean)
  for (const segment of segments) {
    if (segment !== 'admin' && segment !== 'student') {
      const label = TITLE_MAP[segment] || segment
      return `${label} - 智能在线答题系统`
    }
  }
  return '智能在线答题系统'
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
  register: '邮箱注册',
  dashboard: '教学控制台',
  upload: '上传试题',
  questions: '题库管理',
  papers: '试卷管理',
  exams: '考试管理',
  results: '成绩查看',
  review: '评分管理',
  students: '学生管理',
  config: '系统配置',
  feedbacks: '反馈管理',
  announcements: '公告管理',
  practice: '在线做题',
  'practice-history': '练习历史',
  'wrong-book': '错题本',
  profile: '个人中心',
}

router.afterEach((to) => {
  document.title = getRouteTitle(to)
})

export default router
