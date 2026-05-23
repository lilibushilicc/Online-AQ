<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import StatCards from '@/views/components/StatCards.vue'
import { useExamStore, type ResultDetail } from '@/stores/exam'
import * as api from '@/api'
import { downloadFile } from '@/utils/download'
import type { ExamResult } from '@/types'

const store = useExamStore()
const examId = ref<number>()
const detailVisible = ref(false)
const resultDetail = ref<ResultDetail | null>(null)
const resultSearch = ref('')
const filteredResults = computed(() => {
  if (!resultSearch.value) return store.results
  const kw = resultSearch.value.toLowerCase()
  return store.results.filter((r) => {
    const exam = store.exams.find((e) => e.examId === r.examId)
    return exam?.examName.toLowerCase().includes(kw) || String(r.studentId).includes(kw)
  })
})

const averageScore = computed(() => {
  if (store.results.length === 0) return 0
  const total = store.results.reduce((sum, item) => sum + Number(item.totalScore), 0)
  return Math.round((total / store.results.length) * 10) / 10
})
const highestScore = computed(() => Math.max(0, ...store.results.map((item) => Number(item.totalScore))))
const correctRate = computed(() => {
  const correct = store.results.reduce((sum, item) => sum + item.correctCount, 0)
  const total = store.results.reduce((sum, item) => sum + item.correctCount + item.wrongCount, 0)
  return total === 0 ? 0 : Math.round((correct / total) * 100)
})

onMounted(async () => {
  try {
    await store.loadExams()
    examId.value = store.exams[0]?.examId
    if (examId.value) await store.loadExamResults(examId.value)
  } catch {
    ElMessage.error('加载成绩数据失败，请刷新重试')
  }
})

async function loadResults() {
  if (examId.value) await store.loadExamResults(examId.value)
}

async function openDetail(resultId: number) {
  resultDetail.value = await api.getResultDetailApi(resultId)
  detailVisible.value = true
}
</script>

<template>
    <StatCards :items="[
      { title: '提交人数', value: store.results.length, suffix: '人' },
      { title: '平均分', value: averageScore, suffix: '分' },
      { title: '最高分', value: highestScore, suffix: '分' },
      { title: '整体正确率', value: correctRate, suffix: '%' },
    ]" />

    <el-card style="margin-bottom: 14px">
      <template #header>
        <strong>成绩分布解读</strong>
      </template>
      <p style="color: var(--text-tertiary); font-size: 13px; margin: 0 0 16px">如果平均分偏低，可以回到题库或考试配置页面调整难度、开放时间和重考策略。</p>
      <div style="display: grid; gap: 16px">
        <div style="display: grid; grid-template-columns: 80px 1fr 60px; gap: 12px; align-items: center">
          <span style="color: var(--text-tertiary); font-size: 13px">正确率</span>
          <el-progress :percentage="correctRate" :stroke-width="10" :show-text="false" />
          <strong>{{ correctRate }}%</strong>
        </div>
        <div style="display: grid; grid-template-columns: 80px 1fr 60px; gap: 12px; align-items: center">
          <span style="color: var(--text-tertiary); font-size: 13px">平均分</span>
          <el-progress :percentage="Math.min(100, averageScore)" :stroke-width="10" :show-text="false" color="#b56b12" />
          <strong>{{ averageScore }}</strong>
        </div>
        <div style="display: grid; grid-template-columns: 80px 1fr 60px; gap: 12px; align-items: center">
          <span style="color: var(--text-tertiary); font-size: 13px">最高分</span>
          <el-progress :percentage="Math.min(100, highestScore)" :stroke-width="10" :show-text="false" color="#138a5b" />
          <strong>{{ highestScore }}</strong>
        </div>
      </div>
    </el-card>

    <el-card>
      <div class="toolbar">
        <el-select v-model="examId" placeholder="选择考试" style="width: 200px" @change="loadResults">
          <el-option v-for="exam in store.exams" :key="exam.examId" :label="exam.examName" :value="exam.examId" />
        </el-select>
        <el-input v-model="resultSearch" placeholder="搜索考试名称..." clearable style="max-width: 200px" />
        <span class="toolbar-right">
          <span class="muted">共 {{ store.results.length }} 条</span>
          <el-button
            v-if="examId"
            size="small"
            plain
            @click="downloadFile(`/results/export/${examId}`, `考试成绩_${store.exams.find(e => e.examId === examId)?.examName ?? examId}_${new Date().toLocaleDateString()}.xlsx`)"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14" style="margin-right: 4px"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
            导出成绩
          </el-button>
        </span>
      </div>
      <el-empty v-if="!store.results.length" description="暂无成绩，先用学生账号提交一次考试" />
      <el-empty v-else-if="!filteredResults.length && resultSearch" description="没有匹配的成绩" />
      <el-table v-else :data="filteredResults" stripe>
        <el-table-column prop="studentId" label="学生ID" />
        <el-table-column prop="examId" label="考试ID" />
        <el-table-column label="得分">
          <template #default="{ row }">
            <strong>{{ row.totalScore }}</strong>
          </template>
        </el-table-column>
        <el-table-column prop="correctCount" label="正确题数" />
        <el-table-column prop="wrongCount" label="错误题数" />
        <el-table-column prop="submitTime" label="提交时间" min-width="180" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row.resultId)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-drawer v-model="detailVisible" title="成绩详情" size="48%">
      <template v-if="resultDetail">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="考试">{{ resultDetail.exam.examName }}</el-descriptions-item>
          <el-descriptions-item label="得分">{{ resultDetail.result.totalScore }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ new Date(resultDetail.result.submitTime).toLocaleString() }}</el-descriptions-item>
        </el-descriptions>

        <el-divider />

        <div class="question-list">
          <el-card v-for="(answer, index) in resultDetail.answers" :key="answer.questionId" shadow="hover" style="margin-bottom: 14px">
            <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px">
              <strong>{{ index + 1 }}. {{ answer.questionContent }}</strong>
              <el-tag v-if="answer.reviewStatus === 'pending_review'" type="warning">待评分</el-tag>
              <el-tag v-else :type="answer.isCorrect ? 'success' : 'danger'">{{ answer.isCorrect ? '正确' : '错误' }}</el-tag>
            </div>
            <div style="display: flex; flex-wrap: wrap; gap: 8px">
              <el-tag>学生答案：{{ answer.studentAnswer || '未作答' }}</el-tag>
              <el-tag type="success">正确答案：{{ answer.correctAnswer }}</el-tag>
              <el-tag>{{ answer.score }} 分</el-tag>
            </div>
          </el-card>
        </div>

        <el-divider />

        <div class="question-list">
          <el-card v-for="history in resultDetail.history" :key="history.historyId" shadow="hover" style="margin-bottom: 14px">
            <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px">
              <strong>{{ history.actionType }}</strong>
              <span class="muted">{{ history.operatorName || `用户 #${history.operatorId ?? '-'}` }} · {{ new Date(history.createTime).toLocaleString() }}</span>
            </div>
            <p class="muted">{{ history.actionDetail }}</p>
          </el-card>
        </div>
      </template>
    </el-drawer>
</template>
