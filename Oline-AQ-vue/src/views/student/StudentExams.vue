<script setup lang="ts">
import { computed, onMounted } from 'vue'
import StudentLayout from './StudentLayout.vue'
import { useExamStore, type Exam } from '@/stores/exam'

const store = useExamStore()

function toTimeLabel(value?: string | null) {
  if (!value) {
    return '未设置'
  }
  return new Date(value).toLocaleString()
}

function getAvailability(exam: Exam) {
  const now = Date.now()
  const start = exam.startTime ? new Date(exam.startTime).getTime() : null
  const end = exam.endTime ? new Date(exam.endTime).getTime() : null

  if (exam.status !== 'published') {
    return { label: '未发布', type: 'info' as const }
  }
  if (start !== null && now < start) {
    return { label: '未开始', type: 'warning' as const }
  }
  if (end !== null && now > end) {
    return { label: '已结束', type: 'danger' as const }
  }
  return { label: '可参加', type: 'success' as const }
}

function getExamHistory(examId: number) {
  return store.results.filter((result) => result.examId === examId)
}

function canStartExam(exam: Exam) {
  if (!store.isExamAccessible(exam)) {
    return false
  }
  if (exam.allowRetake) {
    return true
  }
  return !store.hasSubmittedExam(exam.examId)
}

const availableCount = computed(() => store.exams.filter((exam) => canStartExam(exam)).length)
const historyCount = computed(() => store.results.length)

onMounted(async () => {
  await Promise.all([store.loadExams(), store.loadMyResults()])
})
</script>

<template>
  <StudentLayout title="考试列表" subtitle="查看考试开放时间、是否允许重考，以及你自己的历史提交记录。">
    <section class="stat-grid">
      <div class="stat-card"><span>当前可参加</span><strong>{{ availableCount }}</strong></div>
      <div class="stat-card"><span>已发布考试</span><strong>{{ store.publishedExams.length }}</strong></div>
      <div class="stat-card"><span>历史提交</span><strong>{{ historyCount }}</strong></div>
      <div class="stat-card"><span>当前身份</span><strong>{{ store.currentUser?.realName || '学生' }}</strong></div>
    </section>

    <section class="exam-grid">
      <article v-for="exam in store.publishedExams" :key="exam.examId" class="exam-tile">
        <div class="toolbar" style="margin-bottom: 0">
          <div>
            <h3>{{ exam.examName }}</h3>
            <p class="muted">{{ exam.description || '暂无考试说明' }}</p>
          </div>
          <el-tag :type="getAvailability(exam).type">{{ getAvailability(exam).label }}</el-tag>
        </div>

        <div class="status-line">
          <div class="status-box"><span class="muted">考试时长</span><b>{{ exam.duration }} 分钟</b></div>
          <div class="status-box"><span class="muted">总分</span><b>{{ exam.totalScore }} 分</b></div>
          <div class="status-box"><span class="muted">重考策略</span><b>{{ exam.allowRetake ? '允许重考' : '仅限一次' }}</b></div>
        </div>

        <div class="status-line">
          <div class="status-box"><span class="muted">开始时间</span><b>{{ toTimeLabel(exam.startTime) }}</b></div>
          <div class="status-box"><span class="muted">结束时间</span><b>{{ toTimeLabel(exam.endTime) }}</b></div>
          <div class="status-box">
            <span class="muted">历史记录</span>
            <b>{{ getExamHistory(exam.examId).length }} 次</b>
          </div>
        </div>

        <div v-if="getExamHistory(exam.examId).length > 0" class="tag-row">
          <el-tag v-for="item in getExamHistory(exam.examId).slice(0, 3)" :key="item.resultId" type="info">
            {{ item.submitTime }} / {{ item.totalScore }} 分
          </el-tag>
        </div>

        <div class="toolbar" style="margin-bottom: 0">
          <span class="muted">
            {{ !exam.allowRetake && store.hasSubmittedExam(exam.examId) ? '该考试已提交过，当前不可再次作答。' : '请在开放时间内完成作答。' }}
          </span>
          <el-button type="primary" :disabled="!canStartExam(exam)" @click="$router.push(`/student/exams/${exam.examId}`)">
            开始答题
          </el-button>
        </div>
      </article>
      <el-empty v-if="store.publishedExams.length === 0" description="暂无已发布考试" />
    </section>
  </StudentLayout>
</template>
