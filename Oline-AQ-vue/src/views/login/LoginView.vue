<script setup lang="ts">
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UploadFilled, Document, EditPen, Trophy } from '@element-plus/icons-vue'
import { useExamStore, type Role } from '@/stores/exam'

const router = useRouter()
const store = useExamStore()

const form = reactive({
  username: 'admin',
  password: '123456',
  role: 'admin' as Role,
})

const features = [
  { icon: UploadFilled, label: '文件导入与自动解析' },
  { icon: Document, label: '灵活组卷与考试管理' },
  { icon: EditPen, label: '在线作答与倒计时' },
  { icon: Trophy, label: '自动评分与成绩分析' },
]

async function submit() {
  try {
    const user = await store.login(form.username, form.password, form.role)
    router.push(user.role === 'admin' ? '/admin/dashboard' : '/student/exams')
  } catch (error) {
    const message = error instanceof Error ? error.message : '登录失败，请稍后重试'
    ElMessage.error(message)
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-hero">
      <div class="hero-badge">在线考试平台 v1.0</div>
      <h1>智能<br><span>在线答题</span>系统</h1>
      <p class="hero-sub">教师导入试题、自动解析组卷并发布考试。学生在线作答，系统即时评分并沉淀数据，形成完整的教学测验闭环。</p>
      <div class="hero-features">
        <div v-for="item in features" :key="item.label" class="hero-feature">
          <span class="hero-feature-icon">
            <el-icon :size="15"><component :is="item.icon" /></el-icon>
          </span>
          <span>{{ item.label }}</span>
        </div>
      </div>
    </section>

    <section class="login-card">
      <h2>登录系统</h2>
      <p class="muted">选择身份，使用测试账号快速体验</p>

      <el-form label-position="top" style="margin-top: 22px" @submit.prevent>
        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="admin / 2023001" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="输入密码" />
        </el-form-item>
        <el-form-item label="身份">
          <el-segmented
            v-model="form.role"
            :options="[
              { label: '教师', value: 'admin' },
              { label: '学生', value: 'student' },
            ]"
          />
        </el-form-item>
        <el-button type="primary" class="login-submit" @click="submit">登录</el-button>
      </el-form>

      <div class="login-footer">
        <span class="muted">测试账号 · admin / 2023001 &nbsp; 密码 123456</span>
      </div>
    </section>
  </main>
</template>
