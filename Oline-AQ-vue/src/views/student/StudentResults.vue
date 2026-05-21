<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import StatCards from '@/views/components/StatCards.vue'
import { useExamStore } from '@/stores/exam'
import { downloadFile } from '@/utils/download'

const store = useExamStore()
const loading = ref(true)
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
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div v-loading="loading" class="results-page">
    <StatCards :items="[
      { title: '提交次数', value: store.results.length, suffix: '次' },
      { title: '最近得分', value: latestScore, suffix: '分' },
      { title: '最高得分', value: bestScore, suffix: '分' },
      { title: '平均得分', value: averageScore, suffix: '分' },
    ]" />

    <div class="results-toolbar" v-if="store.results.length > 0">
      <span class="muted">共 {{ store.results.length }} 条成绩记录</span>
      <el-button size="small" plain @click="downloadFile('/results/export/my', `我的成绩_${new Date().toLocaleDateString()}.xlsx`)">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14" style="margin-right: 4px"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
        导出成绩
      </el-button>
    </div>

    <el-empty v-if="store.results.length === 0" description="暂无成绩，请先参加考试" />

    <el-row :gutter="16" v-else>
      <el-col v-for="result in store.results" :key="result.resultId" :xs="24" :md="12" class="result-col">
        <el-card shadow="never" class="result-card">
          <div class="result-card__inner">
            <el-progress
              type="circle"
              :percentage="Math.min(100, Number(result.totalScore))"
              :stroke-width="8"
              :width="96"
              class="result-progress"
            >
              <span class="result-progress__score">{{ result.totalScore }}</span>
            </el-progress>

            <div class="result-card__body">
              <strong class="result-card__name">{{ getExamName(result.examId) }}</strong>
              <p class="result-card__time">提交时间：{{ new Date(result.submitTime).toLocaleString() }}</p>

              <div class="result-tags">
                <el-tag type="success" size="small" effect="plain">正确 {{ result.correctCount }}</el-tag>
                <el-tag type="danger" size="small" effect="plain">错误 {{ result.wrongCount }}</el-tag>
                <el-tag size="small" effect="plain">用时 {{ result.useTime || 0 }} 秒</el-tag>
              </div>

              <div class="result-card__footer">
                <span class="result-card__id">记录 ID：{{ result.resultId }}</span>
                <el-button type="primary" plain size="small" @click="$router.push(`/student/results/${result.resultId}`)">查看详情</el-button>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.results-page {
  min-height: 200px;
}

.results-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 24px;
  margin-bottom: 16px;
}

.result-col {
  margin-bottom: 16px;
}

.result-card__inner {
  display: flex;
  align-items: center;
  gap: 20px;
}

.result-progress {
  flex-shrink: 0;
}

.result-progress__score {
  font-size: 22px;
  font-weight: 800;
  color: var(--accent-blue);
}

.result-card__body {
  flex: 1;
  min-width: 0;
}

.result-card__name {
  font-size: var(--text-card-title);
  font-weight: 600;
  color: var(--text-primary);
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-card__time {
  color: var(--text-tertiary);
  font-size: var(--text-caption);
  margin: 4px 0 0;
}

.result-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}

.result-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border-subtle);
}

.result-card__id {
  color: var(--text-tertiary);
  font-size: var(--text-small);
}
</style>
