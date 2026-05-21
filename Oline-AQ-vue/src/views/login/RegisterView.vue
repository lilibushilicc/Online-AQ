<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UploadFilled, Document, EditPen, Trophy } from '@element-plus/icons-vue'
import { useExamStore } from '@/stores/exam'

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
      <h2>邮箱注册</h2>
      <p class="login-muted">填写邮箱获取验证码完成注册</p>

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
        <el-button class="login-submit" @click="doRegister">注 册</el-button>
      </el-form>

      <div class="login-footer">
        <el-button link type="primary" @click="router.push('/login')">已有账号？返回登录</el-button>
      </div>
    </section>
  </main>
</template>

<style scoped>
.verif-code-row {
  display: flex; gap: 10px; width: 100%;
}
.verif-code-row .el-input { flex: 1; }
.verif-btn { width: 116px; flex-shrink: 0; }
</style>
