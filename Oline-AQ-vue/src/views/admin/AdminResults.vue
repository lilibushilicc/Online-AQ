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
    <section class="stat-grid">
      <div class="stat-card"><span>提交人数</span><strong>{{ store.results.length }}</strong></div>
      <div class="stat-card"><span>平均分</span><strong>{{ averageScore }}</strong></div>
      <div class="stat-card"><span>最高分</span><strong>{{ highestScore }}</strong></div>
      <div class="stat-card"><span>整体正确率</span><strong>{{ correctRate }}%</strong></div>
    </section>

    <section class="data-strip">
      <div class="data-strip-main">
        <span class="muted">成绩分布解读</span>
        <strong>{{ correctRate }}%</strong>
        <p class="muted">如果平均分偏低，可以回到题库或考试配置页面调整难度、开放时间和重考策略。</p>
      </div>
      <div class="bars">
        <div class="bar-row">
          <span>正确率</span>
          <div class="bar-track"><span class="bar-fill" :style="{ width: `${correctRate}%` }"></span></div>
          <b>{{ correctRate }}%</b>
        </div>
        <div class="bar-row">
          <span>平均分</span>
          <div class="bar-track"><span class="bar-fill" :style="{ width: `${Math.min(100, averageScore)}%` }"></span></div>
          <b>{{ averageScore }}</b>
        </div>
        <div class="bar-row">
          <span>最高分</span>
          <div class="bar-track"><span class="bar-fill" :style="{ width: `${Math.min(100, highestScore)}%` }"></span></div>
          <b>{{ highestScore }}</b>
        </div>
      </div>
    </section>

    <section class="panel">
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
    </section>

    <el-drawer v-model="detailVisible" title="成绩详情" size="48%">
      <template v-if="resultDetail">
        <div class="status-line">
          <div class="status-box"><span class="muted">考试</span><b>{{ resultDetail.exam.examName }}</b></div>
          <div class="status-box"><span class="muted">得分</span><b>{{ resultDetail.result.totalScore }}</b></div>
          <div class="status-box"><span class="muted">提交时间</span><b>{{ new Date(resultDetail.result.submitTime).toLocaleString() }}</b></div>
        </div>

        <el-divider />

        <div class="question-list">
          <article v-for="(answer, index) in resultDetail.answers" :key="answer.questionId" class="question-card">
            <div class="toolbar" style="margin-bottom: 10px">
              <h3>{{ index + 1 }}. {{ answer.questionContent }}</h3>
              <el-tag :type="answer.isCorrect ? 'success' : 'danger'">{{ answer.isCorrect ? '正确' : '错误' }}</el-tag>
            </div>
            <div class="tag-row">
              <el-tag>学生答案：{{ answer.studentAnswer || '未作答' }}</el-tag>
              <el-tag type="success">正确答案：{{ answer.correctAnswer }}</el-tag>
              <el-tag>{{ answer.score }} 分</el-tag>
            </div>
          </article>
        </div>

        <el-divider />

        <div class="question-list">
          <article v-for="history in resultDetail.history" :key="history.historyId" class="question-card">
            <div class="toolbar" style="margin-bottom: 6px">
              <strong>{{ history.actionType }}</strong>
              <span class="muted">{{ history.operatorName || `用户 #${history.operatorId ?? '-'}` }} · {{ new Date(history.createTime).toLocaleString() }}</span>
            </div>
            <p class="muted">{{ history.actionDetail }}</p>
          </article>
        </div>
      </template>
    </el-drawer>
  </AdminLayout>
</template>
