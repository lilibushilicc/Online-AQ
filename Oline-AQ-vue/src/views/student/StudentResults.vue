<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import StudentLayout from './StudentLayout.vue'
import StatCards from '@/views/components/StatCards.vue'
import { useExamStore } from '@/stores/exam'

const store = useExamStore()
const bestScore = computed(() => Math.max(0, ...store.results.map((result) => Number(result.totalScore))))
const averageScore = computed(() => {
  if (store.results.length === 0) return 0
  const total = store.results.reduce((sum, result) => sum + Number(result.totalScore), 0)
  return Math.round((total / store.results.length) * 10) / 10
})
const latestScore = computed(() => Number(store.results[0]?.totalScore ?? 0))

function getExamName(examId: number) {
  return store.exams.find((exam) => exam.examId === examId)?.examName ?? `考试 #${examId}`
}

onMounted(async () => {
  try {
    await Promise.all([store.loadMyResults(), store.loadExams()])
  } catch {
    ElMessage.error('加载成绩数据失败，请刷新重试')
  }
})
</script>

<template>
  <StudentLayout title="我的成绩" subtitle="查看自己的提交历史，并进入每次考试的答题详情与历史记录。">
    <StatCards :items="[
      { title: '提交次数', value: store.results.length, suffix: '次' },
      { title: '最近得分', value: latestScore, suffix: '分' },
      { title: '最高得分', value: bestScore, suffix: '分' },
      { title: '平均得分', value: averageScore, suffix: '分' },
    ]" />

    <el-empty v-if="store.results.length === 0" description="暂无成绩，请先参加考试" />
    <div v-else>
      <el-row :gutter="14">
        <el-col v-for="result in store.results" :key="result.resultId" :xs="24" :md="12" style="margin-bottom: 14px">
          <el-card shadow="hover">
            <div style="display: flex; align-items: center; gap: 20px">
              <el-progress type="circle" :percentage="Math.min(100, Number(result.totalScore))" :stroke-width="8" :width="96">
                <span style="font-size: 22px; font-weight: 800; color: var(--primary-dark)">{{ result.totalScore }}</span>
              </el-progress>
              <div style="flex: 1; min-width: 0">
                <strong>{{ getExamName(result.examId) }}</strong>
                <p style="color: var(--muted); font-size: 13px; margin: 4px 0">提交时间：{{ new Date(result.submitTime).toLocaleString() }}</p>
                <div style="display: flex; flex-wrap: wrap; gap: 8px; margin: 8px 0">
                  <el-tag type="success">正确 {{ result.correctCount }}</el-tag>
                  <el-tag type="danger">错误 {{ result.wrongCount }}</el-tag>
                  <el-tag>用时 {{ result.useTime || 0 }} 秒</el-tag>
                </div>
                <div style="display: flex; align-items: center; justify-content: space-between">
                  <span style="color: var(--muted); font-size: 13px">记录 ID：{{ result.resultId }}</span>
                  <el-button type="primary" plain @click="$router.push(`/student/results/${result.resultId}`)">查看详情</el-button>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </StudentLayout>
</template>
