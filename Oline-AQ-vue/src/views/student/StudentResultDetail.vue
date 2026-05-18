<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import StudentLayout from './StudentLayout.vue'
import { useExamStore, type ResultDetail } from '@/stores/exam'

const route = useRoute()
const store = useExamStore()
const resultDetail = ref<ResultDetail | null>(null)
const resultId = Number(route.params.resultId)

const scorePercent = computed(() => {
  if (!resultDetail.value?.exam.totalScore) {
    return 0
  }

  return Math.round((Number(resultDetail.value.result.totalScore) / Number(resultDetail.value.exam.totalScore)) * 100)
})

onMounted(async () => {
  resultDetail.value = await store.getResultDetail(resultId)
})
</script>

<template>
  <StudentLayout
    :title="resultDetail?.exam.examName ?? '成绩详情加载中'"
    :subtitle="resultDetail ? `成绩 ${resultDetail.result.totalScore} / ${resultDetail.exam.totalScore}，正确率 ${scorePercent}%` : ''"
  >
    <section v-if="resultDetail">
      <el-row :gutter="14">
        <el-col :xs="24" :sm="12" :lg="6">
          <el-card shadow="hover" style="margin-bottom: 14px">
            <el-statistic title="总分" :value="resultDetail.result.totalScore">
              <template #suffix><span style="font-size: 14px; color: var(--muted)">分</span></template>
            </el-statistic>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :lg="6">
          <el-card shadow="hover" style="margin-bottom: 14px">
            <el-statistic title="正确题数" :value="resultDetail.result.correctCount">
              <template #suffix><span style="font-size: 14px; color: var(--muted)">道</span></template>
            </el-statistic>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :lg="6">
          <el-card shadow="hover" style="margin-bottom: 14px">
            <el-statistic title="错误题数" :value="resultDetail.result.wrongCount">
              <template #suffix><span style="font-size: 14px; color: var(--muted)">道</span></template>
            </el-statistic>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :lg="6">
          <el-card shadow="hover" style="margin-bottom: 14px">
            <el-statistic title="正确率" :value="scorePercent">
              <template #suffix><span style="font-size: 14px; color: var(--muted)">%</span></template>
            </el-statistic>
          </el-card>
        </el-col>
      </el-row>
    </section>

    <el-card v-if="resultDetail" style="margin-bottom: 14px">
      <template #header>
        <div style="display: flex; align-items: center; justify-content: space-between">
          <strong>答题明细</strong>
          <span style="color: var(--muted); font-size: 13px">记录 ID：{{ resultDetail.result.resultId }}</span>
        </div>
      </template>
      <el-card v-for="(answer, index) in resultDetail.answers" :key="answer.questionId" shadow="hover" style="margin-bottom: 14px">
        <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px">
          <strong>{{ index + 1 }}. {{ answer.questionContent }}</strong>
          <el-tag :type="answer.isCorrect ? 'success' : 'danger'">{{ answer.isCorrect ? '正确' : '错误' }}</el-tag>
        </div>
        <div v-if="answer.questionType === 'single' || answer.questionType === 'judge'" class="option-grid">
          <span>A. {{ answer.optionA }}</span>
          <span>B. {{ answer.optionB }}</span>
          <span v-if="answer.optionC">C. {{ answer.optionC }}</span>
          <span v-if="answer.optionD">D. {{ answer.optionD }}</span>
        </div>
        <div style="display: flex; flex-wrap: wrap; gap: 8px; margin-top: 12px">
          <el-tag>你的答案：{{ answer.studentAnswer || '未作答' }}</el-tag>
          <el-tag type="success">正确答案：{{ answer.correctAnswer }}</el-tag>
          <el-tag>{{ answer.score }} 分</el-tag>
        </div>
      </el-card>
    </el-card>

    <el-card v-if="resultDetail">
      <template #header>
        <div style="display: flex; align-items: center; justify-content: space-between">
          <strong>考试历史记录</strong>
          <span style="color: var(--muted); font-size: 13px">包含创建、发布、关闭和提交事件</span>
        </div>
      </template>
      <el-card v-for="history in resultDetail.history" :key="history.historyId" shadow="hover" style="margin-bottom: 14px">
        <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px">
          <strong>{{ history.actionType }}</strong>
          <span class="muted">{{ history.operatorName || `用户 #${history.operatorId ?? '-'}` }} · {{ new Date(history.createTime).toLocaleString() }}</span>
        </div>
        <p class="muted">{{ history.actionDetail }}</p>
      </el-card>
    </el-card>
  </StudentLayout>
</template>
