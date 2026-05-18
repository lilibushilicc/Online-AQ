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
    <section class="stat-grid">
      <div class="stat-card"><span>提交次数</span><strong>{{ store.results.length }}</strong></div>
      <div class="stat-card"><span>最近得分</span><strong>{{ latestScore }}</strong></div>
      <div class="stat-card"><span>最高得分</span><strong>{{ bestScore }}</strong></div>
      <div class="stat-card"><span>平均得分</span><strong>{{ averageScore }}</strong></div>
    </section>

    <section class="panel">
      <el-empty v-if="store.results.length === 0" description="暂无成绩，请先参加考试" />
      <div v-else class="question-list">
        <article
          v-for="result in store.results"
          :key="result.resultId"
          class="question-card score-card"
          :style="{ '--score': `${Math.min(100, Number(result.totalScore))}%` }"
        >
          <div class="score-ring">{{ result.totalScore }}</div>
          <div>
            <h3>{{ getExamName(result.examId) }}</h3>
            <p class="muted">提交时间：{{ new Date(result.submitTime).toLocaleString() }}</p>
            <div class="tag-row" style="margin-top: 10px">
              <el-tag type="success">正确 {{ result.correctCount }}</el-tag>
              <el-tag type="danger">错误 {{ result.wrongCount }}</el-tag>
              <el-tag>用时 {{ result.useTime || 0 }} 秒</el-tag>
            </div>
            <div class="toolbar" style="margin: 14px 0 0">
              <span class="muted">历史记录 ID：{{ result.resultId }}</span>
              <el-button type="primary" plain @click="$router.push(`/student/results/${result.resultId}`)">查看详情</el-button>
            </div>
          </div>
        </article>
      </div>
    </section>
  </StudentLayout>
</template>
