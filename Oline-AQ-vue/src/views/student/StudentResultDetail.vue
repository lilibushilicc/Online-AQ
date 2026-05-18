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
    <section v-if="resultDetail" class="stat-grid">
      <div class="stat-card"><span>总分</span><strong>{{ resultDetail.result.totalScore }}</strong></div>
      <div class="stat-card"><span>正确题数</span><strong>{{ resultDetail.result.correctCount }}</strong></div>
      <div class="stat-card"><span>错误题数</span><strong>{{ resultDetail.result.wrongCount }}</strong></div>
      <div class="stat-card"><span>提交时间</span><strong style="font-size: 18px">{{ new Date(resultDetail.result.submitTime).toLocaleString() }}</strong></div>
    </section>

    <section v-if="resultDetail" class="panel">
      <div class="panel-title">
        <h3>答题明细</h3>
        <span class="muted">历史记录 ID：{{ resultDetail.result.resultId }}</span>
      </div>
      <div class="question-list">
        <article v-for="(answer, index) in resultDetail.answers" :key="answer.questionId" class="question-card">
          <div class="toolbar" style="margin-bottom: 10px">
            <h3>{{ index + 1 }}. {{ answer.questionContent }}</h3>
            <el-tag :type="answer.isCorrect ? 'success' : 'danger'">{{ answer.isCorrect ? '正确' : '错误' }}</el-tag>
          </div>
          <div class="option-grid">
            <span>A. {{ answer.optionA }}</span>
            <span>B. {{ answer.optionB }}</span>
            <span v-if="answer.optionC">C. {{ answer.optionC }}</span>
            <span v-if="answer.optionD">D. {{ answer.optionD }}</span>
          </div>
          <div class="tag-row" style="margin-top: 12px">
            <el-tag>你的答案：{{ answer.studentAnswer || '未作答' }}</el-tag>
            <el-tag type="success">正确答案：{{ answer.correctAnswer }}</el-tag>
            <el-tag>{{ answer.score }} 分</el-tag>
          </div>
        </article>
      </div>
    </section>

    <section v-if="resultDetail" class="panel">
      <div class="panel-title">
        <h3>考试历史记录</h3>
        <span class="muted">包含创建、发布、关闭和提交事件</span>
      </div>
      <div class="question-list">
        <article v-for="history in resultDetail.history" :key="history.historyId" class="question-card">
          <div class="toolbar" style="margin-bottom: 6px">
            <strong>{{ history.actionType }}</strong>
            <span class="muted">{{ history.operatorName || `用户 #${history.operatorId ?? '-'}` }} · {{ new Date(history.createTime).toLocaleString() }}</span>
          </div>
          <p class="muted">{{ history.actionDetail }}</p>
        </article>
      </div>
    </section>
  </StudentLayout>
</template>
