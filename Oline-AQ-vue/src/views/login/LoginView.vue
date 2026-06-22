<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElDialog } from 'element-plus'
import { User, Lock, ArrowRight, UploadFilled, Document, EditPen, Trophy } from '@element-plus/icons-vue'
import { useExamStore, type Role } from '@/stores/exam'
import { loadPublicLoginConfigApi } from '@/api'
import CanvasParticles from '@/views/components/CanvasParticles.vue'

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
  <transition name="login-reveal" mode="out-in"><main class="login-page" v-show="true">
    <CanvasParticles />
    <div class="login-logo" @click="handleLogoClick">
      <img src="/loginlogo.png" alt="Online-AQ" class="login-logo-icon" />
    </div>
    <div class="login-container">
      <section class="login-hero login-hero-desktop">
        <div class="hero-tag">
          <span class="hero-tag-dot"></span>
          <span>AI 驱动的考试生态系统</span>
        </div>
        <h1 class="hero-title">全流程智能评分<br><span class="hero-title-accent">重新定义</span>测评体验</h1>
        <p class="hero-sub">依托 AI 驱动的高性能架构，为教育与选拔提供公平、精准、高效的全场景闭环测评解决方案。</p>
        <div class="hero-features">
          <div v-for="(item, idx) in features" :key="item.label" class="hero-feature" :class="`fade-in stagger-${idx + 1}`">
            <span class="hero-feature-icon">
              <el-icon :size="16"><component :is="item.icon" /></el-icon>
            </span>
            <div>
              <p class="hero-feature-title">{{ item.label }}</p>
              <p class="hero-feature-desc">{{ idx === 0 ? '军工级数据加密，全方位护航考试隐私与安全' : idx === 1 ? '多维度多模式 AI 监考，实时洞察考场动态' : idx === 2 ? '在线作答与倒计时，流畅考试体验' : '自动评分与成绩分析，数据驱动教学改进' }}</p>
            </div>
          </div>
        </div>
        <div class="hero-glow-glow"></div>
        <div class="hero-glow-blur"></div>
      </section>
      <section class="login-card">
        <div class="login-card-inner">
          <div class="login-card-header">
            <h2>学生登录</h2>
            <div class="login-card-underline"></div>
          </div>
          <el-form class="login-form" label-position="top" @submit.prevent>
            <el-form-item label="学号 / 邮箱">
              <div class="input-wrap">
                <el-icon class="input-icon"><User /></el-icon>
                <el-input v-model="form.username" placeholder="请输入学号或邮箱" />
              </div>
            </el-form-item>
            <el-form-item label="密码">
              <div class="input-wrap">
                <el-icon class="input-icon"><Lock /></el-icon>
                <el-input v-model="form.password" type="password" show-password placeholder="请输入登录密码" />
              </div>
            </el-form-item>
            <div class="login-forgot-row">
              <a class="login-forgot-link" href="#" @click.prevent>忘记密码？</a>
            </div>
            <el-button class="login-submit" @click="submit">
              <span>登录系统</span>
              <el-icon class="submit-arrow"><ArrowRight /></el-icon>
            </el-button>
          </el-form>
          <div class="login-divider">
            <span>其他方式</span>
          </div>
          <div class="login-card-footer">
            <p class="login-register-text">
              还没有账号？<a class="login-register-link" @click.prevent="router.push('/register')">立即注册</a>
            </p>
            <el-button v-if="loginAdminMethod !== 'click_logo'" class="login-admin-btn" @click="openAdminDialog">
              <el-icon style="margin-right: 6px;"><User /></el-icon>
              <span>管理员入口</span>
            </el-button>
          </div>
        </div>
        <div class="login-footer-links">
          <a @click.prevent>隐私政策</a>
          <a @click.prevent>服务条款</a>
          <a @click.prevent>帮助中心</a>
        </div>
      </section>
    </div>
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
  </main></transition>
</template>

<style scoped>
.login-page {
  min-height: 100dvh;
  position: relative;
  overflow: hidden;
  background: var(--bg-base);
}

.login-container {
  display: flex;
  min-height: 100dvh;
  position: relative;
  z-index: 1;
}

.login-logo {
  position: fixed;
  top: 28px;
  left: 32px;
  z-index: 50;
  cursor: pointer;
  user-select: none;
  -webkit-user-select: none;
  transition: opacity var(--duration-fast) var(--ease-out);
}
.login-logo:hover {
  opacity: 0.85;
}
.login-logo-icon {
  display: block;
  width: 48px;
  height: 48px;
  border-radius: var(--radius);
  object-fit: contain;
  padding: 8px;
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  transition: border-color var(--duration-fast), box-shadow var(--duration-fast);
}
.login-logo:hover .login-logo-icon {
  border-color: rgba(87,241,219,0.25);
  box-shadow: 0 0 20px rgba(87,241,219,0.08);
}

/* ========== Left Hero Side ========== */
.login-hero {
  width: 50%;
  flex-shrink: 0;
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 80px 72px 72px;
  overflow: hidden;
  z-index: 1;
}

.hero-tag {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 16px;
  border-radius: 999px;
  border: 1px solid rgba(87,241,219,0.15);
  background: rgba(87,241,219,0.04);
  color: var(--accent-primary);
  font-size: var(--text-caption);
  font-weight: 600;
  letter-spacing: 0.05em;
  margin-bottom: 28px;
  width: fit-content;
}
.hero-tag-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--accent-primary);
  animation: pulse-ring 2s ease-out infinite;
}

.hero-title {
  margin: 0 0 20px;
  font-size: 48px;
  line-height: 1.1;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--text-primary);
}
.hero-title-accent {
  color: var(--accent-primary);
}

.hero-sub {
  margin: 0 0 44px;
  max-width: 460px;
  font-size: 18px;
  line-height: 1.6;
  color: var(--text-secondary);
}

.hero-features {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hero-feature {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 10px 14px 10px 8px;
  border-radius: var(--radius);
  transition: background var(--duration-fast) var(--ease-out), transform var(--duration-fast) var(--ease-out);
  cursor: default;
}
.hero-feature:hover {
  background: rgba(255,255,255,0.03);
  transform: translateX(8px);
}
.hero-feature-icon {
  width: 40px;
  height: 40px;
  min-width: 40px;
  border-radius: var(--radius-sm);
  background: linear-gradient(135deg, rgba(87,241,219,0.15), rgba(60,221,199,0.1));
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--accent-primary);
  flex-shrink: 0;
  transition: transform var(--duration-fast) var(--ease-out), background var(--duration-fast);
}
.hero-feature:nth-child(2) .hero-feature-icon {
  background: linear-gradient(135deg, rgba(60,221,199,0.15), rgba(87,241,219,0.1));
}
.hero-feature:nth-child(3) .hero-feature-icon {
  background: linear-gradient(135deg, rgba(52,211,153,0.15), rgba(87,241,219,0.1));
}
.hero-feature:nth-child(4) .hero-feature-icon {
  background: linear-gradient(135deg, rgba(251,191,36,0.15), rgba(87,241,219,0.1));
}
.hero-feature:hover .hero-feature-icon {
  transform: scale(1.1);
  background: rgba(255,255,255,0.08);
}
.hero-feature-title {
  margin: 0 0 2px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}
.hero-feature-desc {
  margin: 0;
  font-size: 12px;
  color: var(--text-tertiary);
  line-height: 1.5;
}

.hero-glow-glow {
  position: absolute;
  bottom: -80px;
  left: -80px;
  width: 400px;
  height: 400px;
  border-radius: 50%;
  background: rgba(87,241,219,0.04);
  filter: blur(100px);
  pointer-events: none;
}
.hero-glow-blur {
  position: absolute;
  top: 20%;
  right: -20px;
  width: 300px;
  height: 300px;
  border-radius: 50%;
  background: rgba(87,241,219,0.04);
  filter: blur(80px);
  pointer-events: none;
}

/* ========== Right Login Card ========== */
.login-card {
  width: 50%;
  min-width: 480px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 48px 56px;
  position: relative;
  z-index: 1;
}
.login-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 8%;
  bottom: 8%;
  width: 1px;
  background: linear-gradient(180deg, transparent, rgba(87,241,219,0.1), transparent);
}

.login-card-inner {
  width: 100%;
  max-width: 420px;
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  padding: 44px 40px;
  box-shadow: 0 8px 40px rgba(0,0,0,0.15);
}

.login-card-header {
  margin-bottom: 36px;
}
.login-card-header h2 {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.01em;
}
.login-card-underline {
  width: 32px;
  height: 3px;
  background: var(--accent-primary);
  border-radius: 2px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 22px;
}
.login-form :deep(.el-form-item__label) {
  padding-bottom: 6px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: var(--text-secondary);
}

.input-wrap {
  position: relative;
  width: 100%;
}
.input-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-tertiary);
  z-index: 2;
  font-size: 16px;
  transition: color var(--duration-fast);
  pointer-events: none;
}
.input-wrap:focus-within .input-icon {
  color: var(--accent-primary);
}
.input-wrap :deep(.el-input__wrapper) {
  padding-left: 42px !important;
}

.login-forgot-row {
  text-align: right;
  margin-top: -14px;
  margin-bottom: 20px;
}
.login-forgot-link {
  font-size: 13px;
  color: var(--accent-primary);
  text-decoration: none;
  transition: opacity var(--duration-fast);
}
.login-forgot-link:hover {
  opacity: 0.75;
}

.login-submit {
  width: 100%;
  height: 48px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.3px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: none !important;
  background: var(--gradient-primary) !important;
  color: #fff !important;
  border-radius: var(--radius-sm) !important;
  box-shadow: 0 4px 16px rgba(87,241,219,0.25) !important;
  transition: box-shadow var(--duration-fast), transform var(--duration-fast) var(--ease-out) !important;
  cursor: pointer;
}
.login-submit:hover {
  box-shadow: 0 6px 28px rgba(87,241,219,0.35) !important;
  transform: translateY(-2px);
}
.login-submit:active {
  transform: scale(0.98) !important;
}
.submit-arrow {
  font-size: 16px;
  transition: transform var(--duration-fast) var(--ease-out);
}
.login-submit:hover .submit-arrow {
  transform: translateX(4px);
}

.login-divider {
  position: relative;
  margin: 28px 0;
  text-align: center;
}
.login-divider::before {
  content: '';
  position: absolute;
  inset: 50% 0 0;
  height: 1px;
  background: var(--border-subtle);
}
.login-divider span {
  position: relative;
  padding: 0 16px;
  background: var(--bg-surface);
  color: var(--text-tertiary);
  font-size: 13px;
}

.login-card-footer {
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: center;
}
.login-register-text {
  margin: 0;
  font-size: 14px;
  color: var(--text-secondary);
}
.login-register-link {
  color: var(--accent-primary);
  font-weight: 700;
  cursor: pointer;
  text-decoration: none;
  transition: opacity var(--duration-fast);
}
.login-register-link:hover {
  opacity: 0.75;
}

.login-admin-btn {
  width: 100%;
  height: 44px;
  border: 1px solid var(--border-default) !important;
  border-radius: var(--radius-sm) !important;
  background: transparent !important;
  color: var(--text-secondary) !important;
  font-size: 14px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: all var(--duration-fast) var(--ease-out) !important;
  cursor: pointer;
}
.login-admin-btn:hover {
  border-color: var(--accent-primary) !important;
  background: rgba(87,241,219,0.04) !important;
  color: var(--accent-primary) !important;
}

.login-footer-links {
  margin-top: 28px;
  display: flex;
  gap: 32px;
  justify-content: center;
}
.login-footer-links a {
  font-size: 12px;
  color: var(--text-tertiary);
  cursor: pointer;
  text-decoration: none;
  letter-spacing: 0.02em;
  transition: color var(--duration-fast);
}
.login-footer-links a:hover {
  color: var(--accent-primary);
}

/* ========== Mobile Responsive ========== */
@media (max-width: 1100px) {
  .login-hero-desktop {
    display: none;
  }
  .login-container {
    flex-direction: column;
  }
  .login-card {
    width: 100%;
    min-width: 0;
    padding: 32px 24px 48px;
  }
  .login-card::before {
    display: none;
  }
  .login-card-inner {
    padding: 32px 28px;
  }
  .hero-title {
    font-size: 36px;
  }
  .hero-sub {
    font-size: 16px;
  }
  .login-logo {
    top: 16px;
    left: 20px;
  }
  .login-logo-icon {
    width: 40px;
    height: 40px;
    padding: 6px;
  }
  .hero-tag {
    font-size: 11px;
    padding: 4px 12px;
  }
}

@media (max-width: 640px) {
  .login-card {
    padding: 24px 16px 40px;
  }
  .login-card-inner {
    padding: 24px 20px;
  }
}
</style>
