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
    <div v-loading="loading" style="min-height: 200px">
    <StatCards :items="[
      { title: '提交次数', value: store.results.length, suffix: '次' },
      { title: '最近得分', value: latestScore, suffix: '分' },
      { title: '最高得分', value: bestScore, suffix: '分' },
      { title: '平均得分', value: averageScore, suffix: '分' },
    ]" />

    <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px" v-if="store.results.length > 0">
      <span class="muted">共 {{ store.results.length }} 条成绩记录</span>
      <el-button size="small" plain @click="downloadFile('/results/export/my', `我的成绩_${new Date().toLocaleDateString()}.xlsx`)">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14" style="margin-right: 4px"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
        导出成绩
      </el-button>
    </div>
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
    </div>
</template>
