<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import StudentLayout from './StudentLayout.vue'
import { useExamStore, type ResultDetail } from '@/stores/exam'

const route = useRoute()
const store = useExamStore()
const resultDetail = ref<ResultDetail | null>(null)
const resultId = Number(route.params.resultId)

const scorePercent = computed(() => {
  if (!resultDetail.value?.exam.totalScore) return 0
  return Math.round((Number(resultDetail.value.result.totalScore) / Number(resultDetail.value.exam.totalScore)) * 100)
})

// feedback
const feedbackTarget = ref<{ questionId: number; questionContent: string } | null>(null)
const feedbackVisible = ref(false)
const submittedFeedbackIds = ref<Set<number>>(new Set())
const feedbackForm = ref({ feedbackType: 'answer_error', description: '' })

function openFeedback(answer: { questionId: number; questionContent: string }) {
  feedbackTarget.value = answer
  feedbackForm.value = { feedbackType: 'answer_error', description: '' }
  feedbackVisible.value = true
}

async function submitFeedback() {
  if (!feedbackTarget.value) return
  if (!feedbackForm.value.description.trim()) {
    ElMessage.warning('请填写反馈说明')
    return
  }
  try {
    await store.submitFeedback({
      questionId: feedbackTarget.value.questionId,
      examId: resultDetail.value?.result.examId,
      feedbackType: feedbackForm.value.feedbackType,
      description: feedbackForm.value.description,
    })
    submittedFeedbackIds.value.add(feedbackTarget.value.questionId)
    feedbackVisible.value = false
    ElMessage.success('反馈已提交，感谢你的指正！')
  } catch {
    ElMessage.error('提交失败，请稍后重试')
  }
}

onMounted(async () => {
  resultDetail.value = await store.getResultDetail(resultId)
  const ids = resultDetail.value.answers.map((a) => a.questionId)
  const reported = await store.myFeedbackQuestionIds(ids)
  submittedFeedbackIds.value = new Set(reported)
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
          <div style="display: flex; gap: 8px; align-items: center">
            <el-tag :type="answer.isCorrect ? 'success' : 'danger'">{{ answer.isCorrect ? '正确' : '错误' }}</el-tag>
            <el-button
              v-if="!submittedFeedbackIds.has(answer.questionId)"
              size="small"
              type="warning"
              plain
              @click="openFeedback(answer)"
            >反馈纠错</el-button>
            <el-tag v-else size="small" type="info">已反馈</el-tag>
          </div>
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

    <el-dialog v-model="feedbackVisible" title="题目纠错反馈" width="500px">
      <template v-if="feedbackTarget">
        <p style="margin-bottom: 12px; color: var(--ink);"><strong>{{ feedbackTarget.questionContent }}</strong></p>
        <el-form label-position="top">
          <el-form-item label="反馈类型">
            <el-select v-model="feedbackForm.feedbackType">
              <el-option label="答案错误" value="answer_error" />
              <el-option label="题干错误" value="content_error" />
              <el-option label="选项错误" value="option_error" />
              <el-option label="其他问题" value="other" />
            </el-select>
          </el-form-item>
          <el-form-item label="详细说明">
            <el-input v-model="feedbackForm.description" type="textarea" :rows="4" placeholder="请描述该题目哪里有问题..." />
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="feedbackVisible = false">取消</el-button>
        <el-button type="primary" @click="submitFeedback">提交反馈</el-button>
      </template>
    </el-dialog>
  </StudentLayout>
</template>