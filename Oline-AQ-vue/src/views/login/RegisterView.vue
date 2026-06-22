<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UploadFilled, Document, EditPen, Trophy, User, ArrowRight } from '@element-plus/icons-vue'
import { useExamStore } from '@/stores/exam'
import CanvasParticles from '@/views/components/CanvasParticles.vue'

const router = useRouter()
const store = useExamStore()

const regForm = reactive({
  email: '',
  code: '',
  password: '',
  confirmPassword: '',
  realName: '',
})

const sendingCode = ref(false)
const countdown = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

const features = [
  { icon: UploadFilled, label: '文件导入与自动解析' },
  { icon: Document, label: '灵活组卷与考试管理' },
  { icon: EditPen, label: '在线作答与倒计时' },
  { icon: Trophy, label: '自动评分与成绩分析' },
]

async function sendCode() {
  if (!regForm.email.trim()) {
    ElMessage.warning('请输入邮箱')
    return
  }
  sendingCode.value = true
  try {
    await store.sendCode(regForm.email)
    ElMessage.success('验证码已发送到您的邮箱')
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0 && timer) {
        clearInterval(timer)
        timer = null
      }
    }, 1000)
  } catch (error) {
    const message = error instanceof Error ? error.message : '发送失败'
    ElMessage.error(message)
  } finally {
    sendingCode.value = false
  }
}

async function doRegister() {
  if (!regForm.email.trim() || !regForm.code.trim() || !regForm.password || !regForm.realName.trim()) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (regForm.password.length < 6) {
    ElMessage.warning('密码至少6位')
    return
  }
  if (regForm.password !== regForm.confirmPassword) {
    ElMessage.warning('两次密码不一致')
    return
  }
  try {
    const user = await store.register(regForm.email, regForm.code, regForm.password, regForm.realName)
    ElMessage.success('注册成功')
    router.push('/student/exams')
  } catch (error) {
    const message = error instanceof Error ? error.message : '注册失败'
    ElMessage.error(message)
  }
}
</script>

<template>
  <main class="login-page">
    <CanvasParticles />
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
            <h2>邮箱注册</h2>
            <div class="login-card-underline"></div>
          </div>
          <el-form class="login-form" label-position="top" @submit.prevent>
            <el-form-item label="真实姓名">
              <el-input v-model="regForm.realName" placeholder="请输入真实姓名" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="regForm.email" placeholder="请输入邮箱地址" />
            </el-form-item>
            <el-form-item label="验证码">
              <div class="verif-code-row">
                <el-input v-model="regForm.code" placeholder="6位验证码" maxlength="6" />
                <el-button :disabled="countdown > 0" :loading="sendingCode" @click="sendCode" class="verif-btn">
                  {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
                </el-button>
              </div>
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="regForm.password" type="password" show-password placeholder="密码至少6位" />
            </el-form-item>
            <el-form-item label="确认密码">
              <el-input v-model="regForm.confirmPassword" type="password" show-password placeholder="再次输入密码" />
            </el-form-item>
            <el-button class="login-submit" @click="doRegister">
              <span>注 册</span>
              <el-icon class="submit-arrow"><ArrowRight /></el-icon>
            </el-button>
          </el-form>
          <div class="login-divider">
            <span>已有账号</span>
          </div>
          <div class="login-card-footer">
            <p class="login-register-text">
              已有账号？<a class="login-register-link" @click.prevent="router.push('/login')">返回登录</a>
            </p>
          </div>
        </div>
        <div class="login-footer-links">
          <a @click.prevent>隐私政策</a>
          <a @click.prevent>服务条款</a>
          <a @click.prevent>帮助中心</a>
        </div>
      </section>
    </div>
  </main>
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

.verif-code-row {
  display: flex; gap: 10px; width: 100%;
}
.verif-code-row .el-input { flex: 1; }
.verif-btn { width: 116px; flex-shrink: 0; }

@keyframes pulse-ring {
  0%   { box-shadow: 0 0 0 0 rgba(87,241,219,0.5); }
  70%  { box-shadow: 0 0 0 12px rgba(87,241,219,0); }
  100% { box-shadow: 0 0 0 0 rgba(87,241,219,0); }
}

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
