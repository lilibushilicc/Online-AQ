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
    <el-row :gutter="14">
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" style="margin-bottom: 14px">
          <el-statistic title="当前可参加" :value="availableCount">
            <template #suffix><span style="font-size: 14px; color: var(--muted)">场</span></template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" style="margin-bottom: 14px">
          <el-statistic title="已发布考试" :value="store.publishedExams.length">
            <template #suffix><span style="font-size: 14px; color: var(--muted)">场</span></template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" style="margin-bottom: 14px">
          <el-statistic title="历史提交" :value="historyCount">
            <template #suffix><span style="font-size: 14px; color: var(--muted)">次</span></template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" style="margin-bottom: 14px">
          <el-statistic title="当前身份" :value="store.currentUser?.realName || '学生'" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="14" v-if="store.publishedExams.length > 0">
      <el-col v-for="exam in store.publishedExams" :key="exam.examId" :xs="24" :md="12" style="margin-bottom: 14px">
        <el-card shadow="hover">
          <template #header>
            <div style="display: flex; align-items: center; justify-content: space-between">
              <strong>{{ exam.examName }}</strong>
              <el-tag :type="getAvailability(exam).type">{{ getAvailability(exam).label }}</el-tag>
            </div>
          </template>
          <p style="color: var(--muted); font-size: 13px; margin: 0 0 12px">{{ exam.description || '暂无考试说明' }}</p>
          <el-descriptions :column="3" border size="small">
            <el-descriptions-item label="考试时长">{{ exam.duration }} 分钟</el-descriptions-item>
            <el-descriptions-item label="总分">{{ exam.totalScore }} 分</el-descriptions-item>
            <el-descriptions-item label="重考策略">{{ exam.allowRetake ? '允许重考' : '仅限一次' }}</el-descriptions-item>
            <el-descriptions-item label="开始时间">{{ toTimeLabel(exam.startTime) }}</el-descriptions-item>
            <el-descriptions-item label="结束时间">{{ toTimeLabel(exam.endTime) }}</el-descriptions-item>
            <el-descriptions-item label="历史记录">{{ getExamHistory(exam.examId).length }} 次</el-descriptions-item>
          </el-descriptions>

          <div v-if="getExamHistory(exam.examId).length > 0" style="margin-top: 12px; display: flex; flex-wrap: wrap; gap: 8px">
            <el-tag v-for="item in getExamHistory(exam.examId).slice(0, 3)" :key="item.resultId" type="info">
              {{ item.submitTime }} / {{ item.totalScore }} 分
            </el-tag>
          </div>

          <div style="margin-top: 14px; display: flex; align-items: center; justify-content: space-between">
            <span style="color: var(--muted); font-size: 13px">
              {{ !exam.allowRetake && store.hasSubmittedExam(exam.examId) ? '该考试已提交过，当前不可再次作答。' : '请在开放时间内完成作答。' }}
            </span>
            <el-button type="primary" :disabled="!canStartExam(exam)" @click="$router.push(`/student/exams/${exam.examId}`)">
              开始答题
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-empty v-if="store.publishedExams.length === 0" description="暂无已发布考试" />
  </StudentLayout>
</template>
