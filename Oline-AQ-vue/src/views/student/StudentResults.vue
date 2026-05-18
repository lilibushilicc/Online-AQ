<script setup lang="ts">
import { computed, onMounted } from 'vue'
import StudentLayout from './StudentLayout.vue'
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
  await Promise.all([store.loadMyResults(), store.loadExams()])
})
</script>

<template>
  <StudentLayout title="我的成绩" subtitle="查看自己的提交历史，并进入每次考试的答题详情与历史记录。">
    <el-row :gutter="14">
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" style="margin-bottom: 14px">
          <el-statistic title="提交次数" :value="store.results.length">
            <template #suffix><span style="font-size: 14px; color: var(--muted)">次</span></template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" style="margin-bottom: 14px">
          <el-statistic title="最近得分" :value="latestScore">
            <template #suffix><span style="font-size: 14px; color: var(--muted)">分</span></template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" style="margin-bottom: 14px">
          <el-statistic title="最高得分" :value="bestScore">
            <template #suffix><span style="font-size: 14px; color: var(--muted)">分</span></template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" style="margin-bottom: 14px">
          <el-statistic title="平均得分" :value="averageScore">
            <template #suffix><span style="font-size: 14px; color: var(--muted)">分</span></template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>

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
