<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Download, Document } from '@element-plus/icons-vue'
import StatCards from '@/views/components/StatCards.vue'
import QuestionOptions from '@/views/components/QuestionOptions.vue'
import { useExamStore, type ResultDetail } from '@/stores/exam'
import { downloadFile } from '@/utils/download'
import * as api from '@/api'

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
    await api.submitFeedbackApi({
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
  try {
    resultDetail.value = await api.getResultDetailApi(resultId)
    const ids = resultDetail.value.answers.map((a) => a.questionId)
    const reported = await api.myFeedbackQuestionIdsApi(ids)
    submittedFeedbackIds.value = new Set(reported)
    const fmt = route.query.fromBlank as string | undefined
    if (fmt === 'pdf') setTimeout(pdfExport, 800)
    else if (fmt === 'word') setTimeout(wordExport, 800)
    else if (fmt === 'excel') setTimeout(excelExport, 800)
  } catch {
    ElMessage.error('加载成绩详情失败，请刷新重试')
  }
})

function filename() { return `成绩详情_${new Date().toLocaleDateString()}` }

function excelExport() {
  downloadFile(`/results/export/${resultId}/detail`, `${filename()}.xlsx`)
}

function wordExport() {
  downloadFile(`/results/export/${resultId}/detail/word`, `${filename()}.docx`)
}

async function pdfExport() {
  const el = document.getElementById('result-content')
  if (!el) return
  ElMessage.info('正在生成 PDF…')
  try {
    const html2pdf = (await import('html2pdf.js')).default
    await html2pdf().set({
      margin: [8, 8, 8, 8],
      filename: `${filename()}.pdf`,
      image: { type: 'jpeg', quality: 0.95 },
      html2canvas: { scale: 2, useCORS: true },
      jsPDF: { unit: 'mm', format: 'a4', orientation: 'portrait' },
    }).from(el).save()
  } catch {
    ElMessage.error('PDF 生成失败')
  }
}
</script>

<template>
  <div id="result-content">
    <section v-if="resultDetail">
      <StatCards :items="[
        { title: '总分', value: resultDetail.result.totalScore, suffix: '分' },
        { title: '正确题数', value: resultDetail.result.correctCount, suffix: '道' },
        { title: '错误题数', value: resultDetail.result.wrongCount, suffix: '道' },
        { title: '正确率', value: scorePercent, suffix: '%' },
      ]" />
    </section>

    <el-card v-if="resultDetail" style="margin-bottom: 14px">
      <template #header>
        <div style="display: flex; align-items: center; justify-content: space-between">
          <strong>答题明细</strong>
          <div style="display: flex; align-items: center; gap: 8px">
            <el-button size="small" plain :icon="Download" @click="pdfExport">导出 PDF</el-button>
            <el-button size="small" plain :icon="Document" @click="wordExport">导出 Word</el-button>
            <el-button size="small" plain @click="excelExport">导出 Excel</el-button>
            <span style="color: var(--muted); font-size: 13px">记录 ID：{{ resultDetail.result.resultId }}</span>
          </div>
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
        <QuestionOptions v-if="answer.questionType === 'single' || answer.questionType === 'judge'" :question="answer" />
        <div style="display: flex; flex-wrap: wrap; gap: 8px; margin-top: 12px">
          <el-tag>你的答案：{{ answer.studentAnswer || '未作答' }}</el-tag>
          <el-tag type="success">正确答案：{{ answer.correctAnswer }}</el-tag>
          <el-tag>{{ answer.score }} 分</el-tag>
        </div>
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
</div>
</template>