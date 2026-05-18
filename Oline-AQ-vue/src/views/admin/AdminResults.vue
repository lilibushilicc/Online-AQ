<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AdminLayout from './AdminLayout.vue'
import { useExamStore, type ResultDetail } from '@/stores/exam'

const store = useExamStore()
const examId = ref<number>()
const detailVisible = ref(false)
const resultDetail = ref<ResultDetail | null>(null)

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
  await store.loadExams()
  examId.value = store.exams[0]?.examId
  if (examId.value) await store.loadExamResults(examId.value)
})

async function loadResults() {
  if (examId.value) await store.loadExamResults(examId.value)
}

async function openDetail(resultId: number) {
  resultDetail.value = await store.getResultDetail(resultId)
  detailVisible.value = true
}
</script>

<template>
  <AdminLayout title="成绩查看" subtitle="按考试查看学生成绩，并下钻到每次提交的答题详情和历史记录。">
    <el-row :gutter="14">
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" style="margin-bottom: 14px">
          <el-statistic title="提交人数" :value="store.results.length">
            <template #suffix><span style="font-size: 14px; color: var(--muted)">人</span></template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" style="margin-bottom: 14px">
          <el-statistic title="平均分" :value="averageScore">
            <template #suffix><span style="font-size: 14px; color: var(--muted)">分</span></template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" style="margin-bottom: 14px">
          <el-statistic title="最高分" :value="highestScore">
            <template #suffix><span style="font-size: 14px; color: var(--muted)">分</span></template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" style="margin-bottom: 14px">
          <el-statistic title="整体正确率" :value="correctRate">
            <template #suffix><span style="font-size: 14px; color: var(--muted)">%</span></template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-bottom: 14px">
      <template #header>
        <strong>成绩分布解读</strong>
      </template>
      <p style="color: var(--muted); font-size: 13px; margin: 0 0 16px">如果平均分偏低，可以回到题库或考试配置页面调整难度、开放时间和重考策略。</p>
      <div style="display: grid; gap: 16px">
        <div style="display: grid; grid-template-columns: 80px 1fr 60px; gap: 12px; align-items: center">
          <span style="color: var(--muted); font-size: 13px">正确率</span>
          <el-progress :percentage="correctRate" :stroke-width="10" :show-text="false" />
          <strong>{{ correctRate }}%</strong>
        </div>
        <div style="display: grid; grid-template-columns: 80px 1fr 60px; gap: 12px; align-items: center">
          <span style="color: var(--muted); font-size: 13px">平均分</span>
          <el-progress :percentage="Math.min(100, averageScore)" :stroke-width="10" :show-text="false" color="#b56b12" />
          <strong>{{ averageScore }}</strong>
        </div>
        <div style="display: grid; grid-template-columns: 80px 1fr 60px; gap: 12px; align-items: center">
          <span style="color: var(--muted); font-size: 13px">最高分</span>
          <el-progress :percentage="Math.min(100, highestScore)" :stroke-width="10" :show-text="false" color="#138a5b" />
          <strong>{{ highestScore }}</strong>
        </div>
      </div>
    </el-card>

    <el-card>
      <div class="toolbar">
        <el-select v-model="examId" placeholder="选择考试" style="width: 260px" @change="loadResults">
          <el-option v-for="exam in store.exams" :key="exam.examId" :label="exam.examName" :value="exam.examId" />
        </el-select>
        <span class="muted">共 {{ store.results.length }} 条成绩记录</span>
      </div>
      <el-empty v-if="store.results.length === 0" description="暂无成绩，先用学生账号提交一次考试" />
      <el-table v-else :data="store.results" stripe>
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
              <el-tag :type="answer.isCorrect ? 'success' : 'danger'">{{ answer.isCorrect ? '正确' : '错误' }}</el-tag>
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
  </AdminLayout>
</template>
