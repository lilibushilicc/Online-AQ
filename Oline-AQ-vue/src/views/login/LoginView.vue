<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElDialog } from 'element-plus'
import { UploadFilled, Document, EditPen, Trophy, User } from '@element-plus/icons-vue'
import { useExamStore, type Role } from '@/stores/exam'
import { loadPublicLoginConfigApi } from '@/api'

const router = useRouter()
const store = useExamStore()

const form = reactive({
  username: '',
  password: '',
  role: 'student' as Role,
})

const adminForm = reactive({
  username: '',
  password: '',
})

const features = [
  { icon: UploadFilled, label: '文件导入与自动解析' },
  { icon: Document, label: '灵活组卷与考试管理' },
  { icon: EditPen, label: '在线作答与倒计时' },
  { icon: Trophy, label: '自动评分与成绩分析' },
]

const logoClickCount = ref(0)
let logoTimer: ReturnType<typeof setTimeout> | null = null
const adminDialogVisible = ref(false)
const requiredClickCount = ref(3)
const loginAdminMethod = ref('both')
const logoClickCountLoading = ref(true)

async function loadClickConfig() {
  try {
    const data = await loadPublicLoginConfigApi()
    const val = parseInt(data['login.logo.click.count'] ?? '3', 10)
    if (!isNaN(val) && val > 0) {
      requiredClickCount.value = val
    }
    loginAdminMethod.value = data['login.admin.method'] ?? 'both'
  } catch { }
  logoClickCountLoading.value = false
}

function handleLogoClick() {
  if (logoClickCountLoading.value) return
  if (loginAdminMethod.value === 'direct') return
  logoClickCount.value++
  if (logoTimer) {
    clearTimeout(logoTimer)
  }
  logoTimer = setTimeout(() => {
    logoClickCount.value = 0
  }, 2000)
  if (logoClickCount.value >= requiredClickCount.value) {
    logoClickCount.value = 0
    adminDialogVisible.value = true
  } else {
    const remain = requiredClickCount.value - logoClickCount.value
    ElMessage.info(`还需要点击 ${remain} 次进入管理员登录`)
  }
}

function openAdminDialog() {
  logoClickCount.value = 0
  if (logoTimer) clearTimeout(logoTimer)
  adminDialogVisible.value = true
}

onMounted(() => {
  loadClickConfig()
})

async function submit() {
  if (!form.username.trim()) {
    ElMessage.warning('请输入账号或邮箱')
    return
  }
  if (!form.password) {
    ElMessage.warning('请输入密码')
    return
  }
  try {
    const user = await store.login(form.username, form.password, form.role)
    router.push(user.role === 'admin' ? '/admin/dashboard' : '/student/exams')
  } catch (error) {
    const message = error instanceof Error ? error.message : '登录失败，请稍后重试'
    ElMessage.error(message)
  }
}

async function adminLogin() {
  if (!adminForm.username.trim()) {
    ElMessage.warning('请输入管理员账号')
    return
  }
  if (!adminForm.password) {
    ElMessage.warning('请输入密码')
    return
  }
  try {
    const user = await store.login(adminForm.username, adminForm.password, 'admin')
    adminDialogVisible.value = false
    router.push('/admin/dashboard')
  } catch (error) {
    const message = error instanceof Error ? error.message : '登录失败'
    ElMessage.error(message)
  }
}
</script>

<template>
  <main class="login-page">
    <div class="login-logo" @click="handleLogoClick">
      <img src="/loginlogo.png" alt="Online-AQ" class="login-logo-icon" />
    </div>

    <section class="login-hero">
      <div class="hero-divider"></div>
      <h1>智能<br><span>在线答题</span><br>系统</h1>
      <p class="hero-sub">教师导入试题、自动解析组卷并发布考试。学生在线作答，系统即时评分并沉淀数据，形成完整的教学测验闭环。</p>
      <div class="hero-features">
        <div v-for="(item, idx) in features" :key="item.label" class="hero-feature" :class="`fade-in stagger-${idx + 1}`">
          <span class="hero-feature-icon">
            <el-icon :size="13"><component :is="item.icon" /></el-icon>
          </span>
          <span>{{ item.label }}</span>
        </div>
      </div>
    </section>

    <section class="login-card">
      <h2>学生登录</h2>
      <p class="login-muted">使用学号或邮箱登录</p>

      <el-form class="login-form" label-position="top" @submit.prevent>
        <el-form-item label="学号 / 邮箱">
          <el-input v-model="form.username" placeholder="学号 / 邮箱" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="输入密码" />
        </el-form-item>
        <el-button class="login-submit" @click="submit">登 录</el-button>
      </el-form>

      <div class="login-footer">
        <el-button link type="primary" @click="router.push('/register')">没有账号？邮箱注册</el-button>
        <el-button v-if="loginAdminMethod !== 'click_logo'" link @click="openAdminDialog">
          <el-icon style="margin-right: 4px;"><User /></el-icon>管理员登录
        </el-button>
      </div>
    </section>

    <el-dialog v-model="adminDialogVisible" title="管理员登录" width="400px" top="25vh">
      <el-form label-position="top">
        <el-form-item label="管理员账号">
          <el-input v-model="adminForm.username" placeholder="请输入管理员账号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="adminForm.password" type="password" show-password placeholder="请输入密码" @keyup.enter="adminLogin" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adminDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="adminLogin">登录</el-button>
      </template>
    </el-dialog>
  </main>
</template>
