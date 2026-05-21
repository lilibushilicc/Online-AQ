<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { View } from '@element-plus/icons-vue'
import * as api from '@/api'
import { useExamStore, type Exam, type Question } from '@/stores/exam'
import { downloadFile } from '@/utils/download'

const DRAFT_KEY_PREFIX = 'exam_draft_'

const route = useRoute()
const router = useRouter()
const store = useExamStore()
const examId = Number(route.params.examId)
const answers = ref<Record<number, string>>({})
const exam = ref<Exam | null>(null)
const questions = ref<Question[]>([])
const shuffleMap = ref<Record<number, number[]> | null>(null)
const serverTime = ref(0)
const remainingSeconds = ref(0)
const initialRemainingSeconds = ref(0)
const submitting = ref(false)
const blankDialogVisible = ref(false)
const blankFormat = ref('pdf')
const exportDialogVisible = ref(false)
const exportFormat = ref('word')
const activeQuestionIndex = ref(0)
const hasDraft = ref(false)
let timer: number | null = null
let draftSaveTimer: number | null = null

function draftKey() { return DRAFT_KEY_PREFIX + examId }

function saveDraft() {
  if (!exam.value || submitting.value) return
  const data = { answers: answers.value, examId: examId, savedAt: Date.now() }
  try {
    localStorage.setItem(draftKey(), JSON.stringify(data))
    hasDraft.value = true
  } catch { /* storage full, ignore */ }
}

function loadDraft(): Record<number, string> | null {
  try {
    const raw = localStorage.getItem(draftKey())
    if (!raw) return null
    const data = JSON.parse(raw)
    if (data?.examId === examId && data?.answers) return data.answers
  } catch { /* ignore corrupt draft */ }
  return null
}

function clearDraft() {
  localStorage.removeItem(draftKey())
  hasDraft.value = false
}

const answeredCount = computed(() => Object.keys(answers.value).length)
const progress = computed(() => (questions.value.length === 0 ? 0 : Math.round((answeredCount.value / questions.value.length) * 100)))
const historyCount = computed(() => store.results.filter((result) => result.examId === examId).length)
const elapsedSeconds = computed(() => Math.max(0, initialRemainingSeconds.value - remainingSeconds.value))
const countdownText = computed(() => {
  const hours = Math.floor(remainingSeconds.value / 3600)
  const minutes = Math.floor((remainingSeconds.value % 3600) / 60)
  const seconds = remainingSeconds.value % 60
  return [hours, minutes, seconds].map((value) => String(value).padStart(2, '0')).join(':')
})

function examBlockedReason(currentExam: Exam) {
  if (!store.isExamAccessible(currentExam)) {
    const now = Date.now()
    const start = currentExam.startTime ? new Date(currentExam.startTime).getTime() : null
    const end = currentExam.endTime ? new Date(currentExam.endTime).getTime() : null
    if (start !== null && now < start) return '考试尚未开始'
    if (end !== null && now > end) return '考试已结束'
    return '考试当前不可作答'
  }
  if (!currentExam.allowRetake && store.hasSubmittedExam(currentExam.examId)) return '该考试仅允许提交一次'
  if (!currentExam.duration || currentExam.duration <= 0) return '考试时长未设置，请联系管理员'
  return ''
}

function resolveRemainingSeconds(currentExam: Exam) {
  const durationLimit = Math.max(0, (currentExam.duration || 0) * 60)
  const serverOffset = serverTime.value ? Date.now() - serverTime.value : 0
  const endLimit = currentExam.endTime
    ? Math.max(0, Math.floor((new Date(currentExam.endTime).getTime() - Date.now() + serverOffset) / 1000))
    : Infinity
  if (durationLimit === 0) return endLimit === Infinity ? 0 : endLimit
  if (!currentExam.endTime) return durationLimit
  return Math.min(durationLimit, endLimit)
}

function clearTimer() {
  if (timer !== null) { window.clearInterval(timer); timer = null }
}

function toOriginalAnswer(questionId: number, answer: string): string {
  if (!answer || answer.length !== 1 || !shuffleMap.value) return answer
  const perm = shuffleMap.value[questionId]
  const idx = answer.charCodeAt(0) - 65
  if (perm == null || idx < 0 || idx >= perm.length) return answer
  return String.fromCharCode(65 + perm[idx]!)
}

function scrollToQuestion(index: number) {
  const el = document.getElementById(`question-${index}`)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'center' })
    activeQuestionIndex.value = index
  }
}

function updateActiveQuestion() {
  const cards = questions.value.map((_, i) => document.getElementById(`question-${i}`))
  const viewCenter = window.scrollY + window.innerHeight / 2
  let closest = 0
  let minDist = Infinity
  for (let i = 0; i < cards.length; i++) {
    if (!cards[i]) continue
    const rect = cards[i]!.getBoundingClientRect()
    const cardCenter = window.scrollY + rect.top + rect.height / 2
    const dist = Math.abs(cardCenter - viewCenter)
    if (dist < minDist) { minDist = dist; closest = i }
  }
  activeQuestionIndex.value = closest
}

async function handleSubmit(options?: { auto?: boolean }) {
  if (!exam.value || submitting.value) return
  const autoSubmit = Boolean(options?.auto)
  const blockedReason = examBlockedReason(exam.value)
  if (blockedReason && !autoSubmit) { ElMessage.warning(blockedReason); return }
  if (!autoSubmit && answeredCount.value < questions.value.length) { ElMessage.warning('请完成所有题目后再提交'); return }
  if (!autoSubmit) await ElMessageBox.confirm('提交后将立即自动评分，确认现在提交吗？', '提交试卷', { type: 'warning' })
  submitting.value = true
  clearTimer()
  try {
    const transformedAnswers: Record<number, string> = {}
    for (const [qId, answer] of Object.entries(answers.value)) {
      transformedAnswers[Number(qId)] = toOriginalAnswer(Number(qId), answer)
    }
    const result = await store.submitExam(exam.value.examId, transformedAnswers, elapsedSeconds.value)
    clearDraft()
    if (draftSaveTimer !== null) { window.clearTimeout(draftSaveTimer); draftSaveTimer = null }
    if (autoSubmit) ElMessage.warning('考试时间已到，系统已自动交卷')
    else ElMessage.success(`提交成功，得分 ${result.totalScore}`)
    router.push(`/student/results/${result.resultId}`)
  } finally { submitting.value = false }
}

function openBlankDialog() {
  if (!exam.value || submitting.value) return
  const blockedReason = examBlockedReason(exam.value)
  if (blockedReason) { ElMessage.warning(blockedReason); return }
  blankFormat.value = 'pdf'
  blankDialogVisible.value = true
}

function openExportDialog() {
  if (!exam.value) return
  const blockedReason = examBlockedReason(exam.value)
  if (blockedReason) { ElMessage.warning(blockedReason); return }
  exportFormat.value = 'word'
  exportDialogVisible.value = true
}

function doExport() {
  if (!exam.value) return
  exportDialogVisible.value = false
  if (exportFormat.value === 'pdf') {
    ElMessage.info('PDF 生成中...')
    import('html2pdf.js').then((m) => {
      const el = document.getElementById('export-content')
      if (!el) return
      m.default().set({
        margin: [8, 8, 8, 8],
        filename: `${exam.value!.examName}.pdf`,
        image: { type: 'jpeg', quality: 0.95 },
        html2canvas: { scale: 2, useCORS: true },
        jsPDF: { unit: 'mm', format: 'a4', orientation: 'portrait' },
      }).from(el).save()
    }).catch(() => ElMessage.error('PDF 生成失败'))
  } else if (exportFormat.value === 'word') {
    downloadFile(`/exams/${exam.value.examId}/export/word`, `${exam.value.examName}.docx`)
  } else if (exportFormat.value === 'excel') {
    downloadFile(`/exams/${exam.value.examId}/export/excel`, `${exam.value.examName}.xlsx`)
  }
}

async function handleBlankSubmit() {
  if (!exam.value) return
  blankDialogVisible.value = false
  submitting.value = true
  clearTimer()

  const blankAnswers: Record<number, string> = {}
  questions.value.forEach((q) => { blankAnswers[q.questionId] = '' })

  try {
    const result = await store.submitExam(exam.value.examId, blankAnswers, elapsedSeconds.value)
    clearDraft()
    if (draftSaveTimer !== null) { window.clearTimeout(draftSaveTimer); draftSaveTimer = null }
    ElMessage.success('提交成功，正在加载答案')
    router.push(`/student/results/${result.resultId}?fromBlank=${blankFormat.value}`)
  } finally { submitting.value = false }
}

function startCountdown(currentExam: Exam) {
  initialRemainingSeconds.value = resolveRemainingSeconds(currentExam)
  remainingSeconds.value = initialRemainingSeconds.value
  if (remainingSeconds.value <= 0) {
    const msg = examBlockedReason(currentExam)
    if (msg) { ElMessage.warning(msg); return }
    void handleSubmit({ auto: true })
    return
  }
  clearTimer()
  timer = window.setInterval(() => {
    if (remainingSeconds.value <= 1) { remainingSeconds.value = 0; clearTimer(); void handleSubmit({ auto: true }); return }
    remainingSeconds.value -= 1
  }, 1000)
}

watch(answers, () => {
  if (draftSaveTimer !== null) { window.clearTimeout(draftSaveTimer) }
  draftSaveTimer = window.setTimeout(saveDraft, 800)
}, { deep: true })

onMounted(async () => {
  try {
    await store.loadMyResults()
    const detail = await api.getExamDetailApi(examId)
    exam.value = detail.exam
    questions.value = detail.questions
    if (detail.shuffleMap) shuffleMap.value = detail.shuffleMap
    const d = detail as any
    if (d.serverTime) serverTime.value = d.serverTime
    if (!exam.value) return
    const blockedReason = examBlockedReason(exam.value)
    if (blockedReason) { ElMessage.warning(blockedReason); router.replace('/student/exams'); return }

    const saved = loadDraft()
    if (saved && Object.keys(saved).length > 0) {
      answers.value = saved
      ElMessage.info('已恢复上次作答草稿')
    }

    startCountdown(exam.value)
    window.addEventListener('scroll', updateActiveQuestion, { passive: true })
    window.addEventListener('beforeunload', saveDraft)
  } catch {
    ElMessage.error('加载试卷失败，请返回重试')
  }
})

onUnmounted(() => {
  clearTimer()
  if (draftSaveTimer !== null) { window.clearTimeout(draftSaveTimer) }
  window.removeEventListener('scroll', updateActiveQuestion)
  window.removeEventListener('beforeunload', saveDraft)
})
</script>

<template>
    <section v-if="exam" class="answer-layout">
      <div>
        <el-card shadow="hover" style="margin-bottom: 14px">
          <el-descriptions :column="3" border size="small">
            <el-descriptions-item label="开放开始">{{ exam.startTime ? new Date(exam.startTime).toLocaleString() : '未设置' }}</el-descriptions-item>
            <el-descriptions-item label="开放结束">{{ exam.endTime ? new Date(exam.endTime).toLocaleString() : '未设置' }}</el-descriptions-item>
            <el-descriptions-item label="历史提交">{{ historyCount }} 次</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card
          v-for="(question, index) in questions"
          :key="question.questionId"
          :id="`question-${index}`"
          shadow="hover"
          style="margin-bottom: 14px"
          :body-style="index === activeQuestionIndex ? { borderLeft: '3px solid var(--accent-blue)' } : {}"
        >
          <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px">
            <strong>{{ index + 1 }}. {{ question.questionContent }}</strong>
            <el-tag>{{ question.score }} 分</el-tag>
          </div>
          <template v-if="question.questionType === 'single' || question.questionType === 'judge'">
            <el-radio-group v-model="answers[question.questionId]" style="display: flex; flex-direction: column; gap: 8px">
              <el-radio value="A" style="min-height: 40px; align-items: center">A. {{ question.optionA }}</el-radio>
              <el-radio value="B" style="min-height: 40px; align-items: center">B. {{ question.optionB }}</el-radio>
              <el-radio v-if="question.optionC" value="C" style="min-height: 40px; align-items: center">C. {{ question.optionC }}</el-radio>
              <el-radio v-if="question.optionD" value="D" style="min-height: 40px; align-items: center">D. {{ question.optionD }}</el-radio>
            </el-radio-group>
          </template>
          <template v-else-if="question.questionType === 'fill_blank'">
            <el-input v-model="answers[question.questionId]" placeholder="请输入答案" clearable style="max-width: 400px" />
          </template>
          <template v-else-if="question.questionType === 'short_answer'">
            <el-input v-model="answers[question.questionId]" type="textarea" :rows="4" placeholder="请输入你的回答" />
          </template>
        </el-card>
      </div>
      <aside class="answer-index">
        <strong>答题进度</strong>
        <el-progress :percentage="progress" style="margin: 12px 0" />
        <el-descriptions :column="2" border size="small" style="margin-bottom: 12px">
          <el-descriptions-item label="已答">{{ answeredCount }}</el-descriptions-item>
          <el-descriptions-item label="剩余">{{ questions.length - answeredCount }}</el-descriptions-item>
        </el-descriptions>
        <el-card shadow="never" style="margin-bottom: 10px">
          <el-statistic title="剩余时间" :value="countdownText" />
        </el-card>
        <el-card shadow="never" style="margin-bottom: 10px">
          <el-statistic title="已用时间" :value="`${elapsedSeconds} 秒`" />
        </el-card>
        <div style="margin: 16px 0 18px">
          <div style="font-size: 12px; font-weight: 600; color: var(--text-tertiary); text-transform: uppercase; letter-spacing: 0.4px; margin-bottom: 8px">题目导航</div>
          <div style="display: flex; flex-wrap: wrap; gap: 5px;">
            <button
              v-for="(q, idx) in questions"
              :key="q.questionId"
              class="q-nav-btn"
              :class="{
                'q-nav-btn--done': answers[q.questionId],
                'q-nav-btn--active': idx === activeQuestionIndex,
              }"
              @click="scrollToQuestion(idx)"
            >{{ idx + 1 }}</button>
          </div>
        </div>
        <div v-if="hasDraft" style="font-size: 12px; color: var(--text-tertiary); text-align: center; margin-bottom: 8px">
          草稿已自动保存
        </div>
        <el-button type="primary" size="large" style="width: 100%" :loading="submitting" @click="handleSubmit()">
          提交试卷
        </el-button>
        <el-button size="small" plain style="width: 100%; margin-top: 8px; margin-bottom: 4px" @click="openExportDialog()">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14" style="margin-right: 4px"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
          导出试卷
        </el-button>
        <el-button size="small" plain style="width: 100%; margin-top: 4px" :loading="submitting" @click="openBlankDialog()">
          <el-icon style="margin-right: 4px"><View /></el-icon>查看答案
        </el-button>
      </aside>

      <!-- 空白提交确认弹窗 -->
      <el-dialog v-model="blankDialogVisible" title="查看答案" width="440px" :close-on-click-modal="false">
        <div style="margin-bottom: 20px;">
          <p style="margin: 0 0 8px; color: var(--text-primary); font-size: 14px; line-height: 1.7;">
            提交空白试卷后可查看全部正确答案，本次得分为 0。
          </p>
          <p v-if="exam && !exam.allowRetake" style="margin: 0; color: var(--accent-red); font-size: 13px; font-weight: 600;">
            ⚠ 该考试禁止重考，提交后无法再次作答。
          </p>
        </div>
        <el-form label-position="top">
          <el-form-item label="导出格式">
            <el-radio-group v-model="blankFormat">
              <el-radio value="pdf">PDF</el-radio>
              <el-radio value="word">Word</el-radio>
              <el-radio value="excel">Excel</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="blankDialogVisible = false">返回作答</el-button>
          <el-button type="primary" :loading="submitting" @click="handleBlankSubmit">确认查看</el-button>
        </template>
      </el-dialog>

      <!-- 导出试卷弹窗 -->
      <el-dialog v-model="exportDialogVisible" title="导出试卷" width="400px">
        <p style="margin-bottom: 16px">将当前试卷导出为文件，不包含答案。</p>
        <el-radio-group v-model="exportFormat">
          <el-radio value="word">Word 文档</el-radio>
          <el-radio value="excel">Excel 表格</el-radio>
          <el-radio value="pdf">PDF 文件</el-radio>
        </el-radio-group>
        <template #footer>
          <el-button @click="exportDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="doExport">下载</el-button>
        </template>
      </el-dialog>

      <!-- 隐藏的导出内容（供 html2pdf 抓取） -->
      <div id="export-content" style="position: fixed; left: -9999px; top: 0; width: 210mm; padding: 20mm; background: #fff; color: #000; font-size: 14px; line-height: 1.8" v-if="exam">
        <h1 style="text-align: center; margin-bottom: 20px">{{ exam.examName }}</h1>
        <p v-if="exam.description" style="margin-bottom: 16px; font-style: italic">{{ exam.description }}</p>
        <div v-for="(q, index) in questions" :key="q.questionId" style="margin-bottom: 20px">
          <strong>{{ index + 1 }}. {{ q.questionContent }}</strong>
          <p style="margin: 6px 0 0 12px">A. {{ q.optionA }}</p>
          <p style="margin: 2px 0 0 12px">B. {{ q.optionB }}</p>
          <p v-if="q.optionC" style="margin: 2px 0 0 12px">C. {{ q.optionC }}</p>
          <p v-if="q.optionD" style="margin: 2px 0 0 12px">D. {{ q.optionD }}</p>
          <p style="margin: 4px 0 0 12px; color: #666; font-size: 12px">（{{ q.score }} 分）</p>
        </div>
      </div>
    </section>
    <section v-else>
      <el-empty description="考试不存在或正在加载" />
    </section>
</template>

<style scoped>
.q-nav-btn {
  width: 32px;
  height: 32px;
  border-radius: 4px;
  border: 1px solid var(--border-default);
  background: var(--bg-base);
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--duration-fast);
  display: flex;
  align-items: center;
  justify-content: center;
}
.q-nav-btn:hover {
  border-color: var(--accent-blue);
  background: rgba(59,130,246,0.1);
  color: var(--accent-blue);
}
.q-nav-btn--done {
  background: rgba(59,130,246,0.1);
  border-color: color-mix(in srgb, var(--accent-blue) 25%, transparent);
  color: var(--accent-blue);
}
.q-nav-btn--active {
  border-color: var(--accent-blue);
  background: var(--accent-blue);
  color: #fff;
  box-shadow: 0 2px 6px rgba(0,0,0,0.18);
}
</style>