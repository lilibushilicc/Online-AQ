<script setup lang="ts">
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useExamStore, type Role } from '@/stores/exam'

const router = useRouter()
const store = useExamStore()

const form = reactive({
  username: 'admin',
  password: '123456',
  role: 'admin' as Role,
})

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
      <h1>智能在线答题系统</h1>
      <p>面向教师与学生的在线考试平台：从试题文件导入、自动解析、组卷发布，到在线作答和成绩反馈，形成一条清晰的测验闭环。</p>
      <el-steps :active="5" align-center style="margin-top: 32px">
        <el-step title="文件导入" description="TXT / DOCX" />
        <el-step title="自动解析" description="题干与答案" />
        <el-step title="在线答题" description="学生提交" />
        <el-step title="即时评分" description="成绩反馈" />
        <el-step title="数据沉淀" description="题库与结果" />
      </el-steps>
    </section>

    <section class="login-card">
      <h2>登录系统</h2>
      <p class="muted">教师上传题目并发布考试，学生在线答题后自动评分。</p>

      <el-form label-position="top" style="margin-top: 24px" @submit.prevent>
        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="admin / 2023001" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password />
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
        <el-button type="primary" size="large" style="width: 100%" @click="submit">登录</el-button>
      </el-form>

      <p class="muted">测试账号：admin / 123456，2023001 / 123456</p>
    </section>
  </main>
</template>
