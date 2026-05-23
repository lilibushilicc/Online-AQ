<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useExamStore, type ExamResult } from '@/stores/exam'
import { formatTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const store = useExamStore()
const examId = Number(route.params.examId)

const exam = computed(() => store.exams.find((e) => e.examId === examId))

const submissions = computed(() => {
  return store.results
    .filter((r) => r.examId === examId)
    .sort((a, b) => new Date(b.submitTime).getTime() - new Date(a.submitTime).getTime())
    .map((r, i, arr) => ({
      ...r,
      attemptNumber: arr.length - i,
    }))
})

const bestScore = computed(() => Math.max(...submissions.value.map((s) => Number(s.totalScore))))
const latestScore = computed(() => Number(submissions.value[0]?.totalScore ?? 0))

function goDetail(resultId: number) {
  router.push(`/student/results/${resultId}`)
}

onMounted(async () => {
  if (store.results.length === 0) {
    await store.loadMyResults()
  }
  if (store.exams.length === 0) {
    await store.loadStudentExams()
  }
})
</script>

<template>
  <div v-loading="!exam" class="exam-history-page">
    <!-- 试卷信息头 -->
    <el-card v-if="exam" shadow="hover" class="history-header-card">
      <div class="history-header__top">
        <h2>{{ exam.examName }}</h2>
        <el-tag type="primary" size="small" effect="plain" v-if="exam.allowRetake">可重复考试</el-tag>
        <el-tag type="info" size="small" effect="plain" v-else>已关闭重考</el-tag>
      </div>
      <el-descriptions :column="3" border size="small" style="margin-top: 16px">
        <el-descriptions-item label="提交次数">{{ submissions.length }} 次</el-descriptions-item>
        <el-descriptions-item label="最近得分">{{ latestScore }} 分</el-descriptions-item>
        <el-descriptions-item label="最高得分">{{ bestScore }} 分</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 历次提交 -->
    <div v-if="submissions.length > 0" class="history-list">
      <el-card
        v-for="(sub, idx) in submissions"
        :key="sub.resultId"
        shadow="hover"
        class="history-item"
        @click="goDetail(sub.resultId)"
      >
        <div class="history-item__inner">
          <div class="history-item__info">
            <strong>第 {{ sub.attemptNumber }} 次提交</strong>
            <span class="history-item__time">{{ formatTime(sub.submitTime) }}</span>
            <div class="history-item__tags">
              <el-tag type="success" size="small" effect="plain">正确 {{ sub.correctCount }}</el-tag>
              <el-tag type="danger" size="small" effect="plain">错误 {{ sub.wrongCount }}</el-tag>
              <el-tag size="small" effect="plain">用时 {{ sub.useTime || 0 }} 秒</el-tag>
            </div>
          </div>
          <div class="history-item__score">
            <span class="history-item__score-num">{{ sub.totalScore }}</span>
            <span class="history-item__score-unit">分</span>
          </div>
          <el-icon class="history-item__arrow">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18"><polyline points="9 18 15 12 9 6"/></svg>
          </el-icon>
        </div>
      </el-card>
    </div>

    <el-empty v-if="submissions.length === 0 && exam" description="该试卷暂无提交记录" />
  </div>
</template>

<style scoped>
.exam-history-page {
  min-height: 200px;
}

.history-header-card {
  border: 1px solid var(--accent-blue) !important;
}

.history-header__top {
  display: flex;
  align-items: center;
  gap: 12px;
}

.history-header__top h2 {
  margin: 0;
  font-size: var(--text-page-title);
  font-weight: 700;
  color: var(--text-primary);
}

.history-list {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.history-item {
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
}

.history-item:hover {
  transform: translateX(4px);
  box-shadow: var(--shadow-md) !important;
}

.history-item__inner {
  display: flex;
  align-items: center;
  gap: 16px;
}

.history-item__info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.history-item__info strong {
  font-size: var(--text-card-title);
  color: var(--text-primary);
}

.history-item__time {
  font-size: var(--text-caption);
  color: var(--text-tertiary);
}

.history-item__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.history-item__score {
  text-align: center;
  flex-shrink: 0;
}

.history-item__score-num {
  font-size: 28px;
  font-weight: 800;
  color: var(--accent-blue);
}

.history-item__score-unit {
  font-size: var(--text-small);
  color: var(--text-tertiary);
}

.history-item__arrow {
  color: var(--text-tertiary);
  flex-shrink: 0;
}
</style>
