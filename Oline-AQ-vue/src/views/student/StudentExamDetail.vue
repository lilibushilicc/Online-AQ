<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { View } from '@element-plus/icons-vue'
import * as api from '@/api'
import { useExamStore, type Exam, type Question } from '@/stores/exam'
import { QUESTION_TYPE_LABEL } from '@/constants'
import { downloadFile } from '@/utils/download'
import CanvasRadialGauge from '@/views/components/CanvasRadialGauge.vue'

const DRAFT_KEY_PREFIX = 'exam_draft_'

const route = useRoute()
const router = useRouter()
const store = useExamStore()
const examId = Number(route.params.examId)
const answers = ref<Record<number, string>>({})
const exam = ref<Exam | null>(null)
const questions = ref<Question[]>([])
const shuffleMap = ref<Record<number, number[]> | null>(null)
const attemptId = ref<string>('')
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
const restoredFromDraft = ref(false)
const started = ref(false)
const leavingConfirmed = ref(false)
let timer: number | null = null
let draftSaveTimer: number | null = null

function draftKey() { return DRAFT_KEY_PREFIX + examId }

function cacheDraftLocally() {
  if (!attemptId.value) return
  try {
    localStorage.setItem(draftKey(), JSON.stringify({ answers: answers.value, examId, attemptId: attemptId.value, savedAt: Date.now() }))
  } catch { /* ignore */ }
}

async function saveDraft() {
  if (!exam.value || submitting.value || !attemptId.value) return
  const answerList = Object.entries(answers.value).map(([questionId, studentAnswer]) => ({
    questionId: Number(questionId),
    studentAnswer,
  }))
  try {
    await api.saveDraftApi(examId, attemptId.value, answerList, elapsedSeconds.value)
    hasDraft.value = true
  } catch { /* ignore */ }
  cacheDraftLocally()
}

async function loadDraft(): Promise<Record<number, string> | null> {
  try {
    const draft = await api.loadDraftApi(examId)
    if (draft?.answers) {
      const parsed = JSON.parse(draft.answers)
      if (Object.keys(parsed).length > 0) return parsed
    }
  } catch { /* ignore */ }
  try {
    const raw = localStorage.getItem(draftKey())
    if (!raw) return null
    const data = JSON.parse(raw)
    if (data?.examId === examId && data?.attemptId === attemptId.value && data?.answers) return data.answers
  } catch { /* ignore */ }
  return null
}

function startExam() {
  started.value = true
  startCountdown(exam.value!)
  window.addEventListener('scroll', updateActiveQuestion, { passive: true })
}

function clearDraft() {
  api.clearDraftApi(examId).catch(() => {})
  localStorage.removeItem(draftKey())
  hasDraft.value = false
}

const answeredCount = computed(() => Object.values(answers.value).filter((value) => String(value ?? '').trim() !== '').length)
const progress = computed(() => (questions.value.length === 0 ? 0 : Math.round((answeredCount.value / questions.value.length) * 100)))
const historyCount = computed(() => store.results.filter((result) => result.examId === examId).length)
const elapsedSeconds = computed(() => Math.max(0, initialRemainingSeconds.value - remainingSeconds.value))
const shouldBlockLeaving = computed(() => started.value && Boolean(exam.value) && Boolean(attemptId.value) && !submitting.value)
const countdownText = computed(() => {
  const hours = Math.floor(remainingSeconds.value / 3600)
  const minutes = Math.floor((remainingSeconds.value % 3600) / 60)
  const seconds = remainingSeconds.value % 60
  return [hours, minutes, seconds].map((value) => String(value).padStart(2, '0')).join(':')
})

interface QuestionSection {
  type: string
  label: string
  questions: Question[]
  startIndex: number
}

const questionSections = computed(() => {
  const order = ['single', 'judge', 'fill_blank', 'short_answer']
  const sectionLabels: Record<string, string> = {
    single: '一、选择题',
    judge: '二、判断题',
    fill_blank: '三、填空题',
    short_answer: '四、简答题',
  }
  let idx = 0
  const result: QuestionSection[] = []
  for (const type of order) {
    const qs = questions.value.filter((q) => q.questionType === type)
    if (qs.length === 0) continue
    result.push({ type, label: sectionLabels[type] || type, questions: qs, startIndex: idx })
    idx += qs.length
  }
  return result
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
  const unansweredCount = questions.value.length - answeredCount.value
  if (blockedReason && !autoSubmit) { ElMessage.warning(blockedReason); return }
  if (!autoSubmit) {
    const submitMessage = unansweredCount > 0
      ? `您还有 ${unansweredCount} 道题未作答，提交后这些题目将按未作答处理，确认现在提交吗？`
      : '提交后将立即自动评分，确认现在提交吗？'
    await ElMessageBox.confirm(submitMessage, '提交试卷', { type: 'warning' })
  }
  submitting.value = true
  clearTimer()
  try {
    const result = await store.submitExam(exam.value.examId, attemptId.value, answers.value, elapsedSeconds.value)
    clearDraft()
    attemptId.value = ''
    leavingConfirmed.value = true
    if (draftSaveTimer !== null) { window.clearTimeout(draftSaveTimer); draftSaveTimer = null }
    if (autoSubmit) ElMessage.warning('考试时间已到，系统已自动交卷')
    else ElMessage.success(`提交成功，得分 ${result.totalScore}`)
    if (result.wrongCount > 0) {
      try {
        await ElMessageBox.confirm(`您有 ${result.wrongCount} 道错题，是否立即查看并加入错题本？`, '错题提示', {
          confirmButtonText: '查看错题',
          cancelButtonText: '稍后再说',
          type: 'info',
        })
        router.replace(`/student/results/${result.resultId}`)
      } catch {
        router.push(`/student/exams`)
      }
    } else {
      router.replace(`/student/results/${result.resultId}`)
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '提交失败，请稍后重试')
    if (started.value && exam.value) startCountdown(exam.value)
  } finally { submitting.value = false }
}

async function confirmLeaveExam() {
  if (!shouldBlockLeaving.value || leavingConfirmed.value) return true
  try {
    await ElMessageBox.confirm('退出考试后将保留当前作答草稿，下次进入可继续答题。确认现在退出吗？', '退出考试', {
      type: 'warning',
      confirmButtonText: '确认退出',
      cancelButtonText: '继续作答',
    })
    await saveDraft()
    cacheDraftLocally()
    hasDraft.value = true
    leavingConfirmed.value = true
    return true
  } catch {
    return false
  }
}

async function handleExitExam() {
  if (!started.value) {
    router.push('/student/exams')
    return
  }
  if (await confirmLeaveExam()) router.push('/student/exams')
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
    const result = await store.submitExam(exam.value.examId, attemptId.value, blankAnswers, elapsedSeconds.value)
    clearDraft()
    attemptId.value = ''
    leavingConfirmed.value = true
    if (draftSaveTimer !== null) { window.clearTimeout(draftSaveTimer); draftSaveTimer = null }
    ElMessage.success('提交成功，正在加载答案')
    router.replace(`/student/results/${result.resultId}?fromBlank=${blankFormat.value}`)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '提交失败，请稍后重试')
    if (started.value && exam.value) startCountdown(exam.value)
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

function handleBeforeUnload(event: BeforeUnloadEvent) {
  cacheDraftLocally()
  if (!shouldBlockLeaving.value || leavingConfirmed.value) return
  event.preventDefault()
  event.returnValue = ''
}

watch(answers, () => {
  if (draftSaveTimer !== null) { window.clearTimeout(draftSaveTimer) }
  draftSaveTimer = window.setTimeout(saveDraft, 800)
}, { deep: true })

onBeforeRouteLeave(async () => {
  if (await confirmLeaveExam()) return true
  return false
})

onMounted(async () => {
  try {
    await store.loadMyResults()
    const detail = await api.getExamDetailApi(examId, attemptId.value || undefined)
    exam.value = detail.exam
    questions.value = detail.questions
    attemptId.value = detail.attemptId || ''
    if (detail.shuffleMap) shuffleMap.value = detail.shuffleMap
    const d = detail as any
    if (d.serverTime) serverTime.value = d.serverTime
    if (!exam.value) return
    const blockedReason = examBlockedReason(exam.value)
    if (blockedReason) { ElMessage.warning(blockedReason); router.replace('/student/exams'); return }

    const saved = await loadDraft()
    if (saved && Object.keys(saved).length > 0) {
      answers.value = saved
      restoredFromDraft.value = true
      started.value = true
      startCountdown(exam.value)
      window.addEventListener('scroll', updateActiveQuestion, { passive: true })
    }

    window.addEventListener('beforeunload', handleBeforeUnload)
  } catch {
    ElMessage.error('加载试卷失败，请返回重试')
  }
})

onUnmounted(() => {
  clearTimer()
  if (draftSaveTimer !== null) { window.clearTimeout(draftSaveTimer) }
  window.removeEventListener('scroll', updateActiveQuestion)
  window.removeEventListener('beforeunload', handleBeforeUnload)
})
</script>

<template>
    <section v-if="exam" class="answer-layout">

      <!-- 考试前概览 -->
      <div v-if="!started" style="max-width: 600px; margin: 40px auto; text-align: center">
        <el-card>
          <h2 style="margin-bottom: 20px">{{ exam.examName }}</h2>
          <el-descriptions :column="1" border size="small" style="margin-bottom: 20px">
            <el-descriptions-item label="题目数量">{{ questions.length }} 道</el-descriptions-item>
            <el-descriptions-item label="考试时长">{{ exam.duration }} 分钟</el-descriptions-item>
            <el-descriptions-item label="总分">{{ exam.totalScore }} 分</el-descriptions-item>
            <el-descriptions-item label="开放开始">{{ exam.startTime ? new Date(exam.startTime).toLocaleString() : '未设置' }}</el-descriptions-item>
            <el-descriptions-item label="开放结束">{{ exam.endTime ? new Date(exam.endTime).toLocaleString() : '未设置' }}</el-descriptions-item>
          </el-descriptions>
          <div v-if="questionSections.length > 0" style="margin-bottom: 16px">
            <div style="font-size: 14px; font-weight: 600; margin-bottom: 8px; text-align: left">试卷结构</div>
            <div style="display: flex; flex-wrap: wrap; gap: 8px">
              <el-tag
                v-for="section in questionSections"
                :key="section.type"
                :type="section.type === 'single' ? 'primary' : section.type === 'judge' ? 'warning' : section.type === 'fill_blank' ? 'info' : 'success'"
                effect="plain"
                size="large"
              >
                <strong>{{ section.label }} {{ section.questions.length }} 题</strong>
              </el-tag>
            </div>
          </div>
          <el-button type="primary" size="large" @click="startExam">开始作答</el-button>
        </el-card>
      </div>

      <template v-if="started">
      <div>
        <el-card shadow="hover" style="margin-bottom: 14px">
          <el-descriptions :column="3" border size="small">
            <el-descriptions-item label="开放开始">{{ exam.startTime ? new Date(exam.startTime).toLocaleString() : '未设置' }}</el-descriptions-item>
            <el-descriptions-item label="开放结束">{{ exam.endTime ? new Date(exam.endTime).toLocaleString() : '未设置' }}</el-descriptions-item>
            <el-descriptions-item label="历史提交">{{ historyCount }} 次</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-alert
          v-if="restoredFromDraft"
          type="warning"
          :closable="true"
          show-icon
          style="margin-bottom: 14px"
          title="已从草稿恢复"
          :description="`已恢复 ${answeredCount} 道题的作答记录，作答进度将自动保存。`"
        />

        <template v-for="section in questionSections" :key="section.type">
          <el-card shadow="never" class="section-header-card">
            <div style="display: flex; align-items: center; gap: 12px">
              <span class="section-header-label">{{ section.label }}</span>
              <span style="font-size: 13px; color: var(--text-secondary)">共 {{ section.questions.length }} 题</span>
            </div>
          </el-card>
          <el-card
            v-for="(question, localIdx) in section.questions"
            :key="question.questionId"
            :id="`question-${section.startIndex + localIdx}`"
            shadow="hover"
            style="margin-bottom: 14px"
            :body-style="section.startIndex + localIdx === activeQuestionIndex ? { borderLeft: '3px solid var(--el-color-primary)', background: 'var(--el-color-primary-light-9)' } : {}"
          >
            <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px">
              <strong>{{ section.startIndex + localIdx + 1 }}. {{ question.questionContent }}</strong>
              <div style="display: flex; align-items: center; gap: 8px; flex-shrink: 0">
                <el-tag
                  :type="question.questionType === 'single' ? 'primary' : question.questionType === 'judge' ? 'warning' : question.questionType === 'fill_blank' ? 'info' : 'success'"
                  effect="plain"
                  size="large"
                >
                  <strong>{{ QUESTION_TYPE_LABEL[question.questionType] || question.questionType }}</strong>
                </el-tag>
                <el-tag>{{ question.score }} 分</el-tag>
              </div>
            </div>
            <template v-if="question.questionType === 'single' || question.questionType === 'judge'">
              <el-radio-group v-model="answers[question.questionId]" style="display: flex; flex-direction: column; gap: 8px">
                <template v-if="question.questionType === 'judge'">
                  <el-radio value="A" style="min-height: 40px; align-items: center"><span class="judge-option judge-true">对</span></el-radio>
                  <el-radio value="B" style="min-height: 40px; align-items: center"><span class="judge-option judge-false">错</span></el-radio>
                </template>
                <template v-else>
                  <el-radio value="A" style="min-height: 40px; align-items: center">A. {{ question.optionA }}</el-radio>
                  <el-radio value="B" style="min-height: 40px; align-items: center">B. {{ question.optionB }}</el-radio>
                  <el-radio v-if="question.optionC" value="C" style="min-height: 40px; align-items: center">C. {{ question.optionC }}</el-radio>
                  <el-radio v-if="question.optionD" value="D" style="min-height: 40px; align-items: center">D. {{ question.optionD }}</el-radio>
                </template>
              </el-radio-group>
            </template>
            <template v-else-if="question.questionType === 'fill_blank'">
              <el-input v-model="answers[question.questionId]" placeholder="请输入答案" clearable style="max-width: 400px" />
            </template>
            <template v-else-if="question.questionType === 'short_answer'">
              <el-input v-model="answers[question.questionId]" type="textarea" :rows="4" placeholder="请输入你的回答" />
            </template>
          </el-card>
        </template>
      </div>
      <aside class="answer-index">
        <div style="display: flex; flex-direction: column; align-items: center; margin-bottom: 12px">
          <strong style="align-self: flex-start">答题进度</strong>
          <CanvasRadialGauge :value="progress" :size="100" :strokeWidth="7" color="#3b82f6" />
        </div>
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
          <template v-for="section in questionSections" :key="section.type">
            <div class="nav-section-label">{{ section.label }}</div>
            <div style="display: flex; flex-wrap: wrap; gap: 5px; margin-bottom: 10px">
              <button
                v-for="(q, localIdx) in section.questions"
                :key="q.questionId"
                class="q-nav-btn"
                :class="{
                  'q-nav-btn--done': answers[q.questionId],
                  'q-nav-btn--active': section.startIndex + localIdx === activeQuestionIndex,
                }"
                @click="scrollToQuestion(section.startIndex + localIdx)"
              >{{ section.startIndex + localIdx + 1 }}</button>
            </div>
          </template>
        </div>
        <div v-if="hasDraft" style="font-size: 12px; color: var(--text-tertiary); text-align: center; margin-bottom: 8px">
          草稿已自动保存
        </div>
        <el-button type="primary" size="large" style="width: 100%" :loading="submitting" @click="handleSubmit()">
          提交试卷
        </el-button>
        <el-button size="large" plain style="width: 100%; margin-top: 8px" :disabled="submitting" @click="handleExitExam">
          退出考试
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
        <template v-for="section in questionSections" :key="section.type">
          <h2 style="margin: 20px 0 12px; color: #333; font-size: 16px; border-bottom: 1px solid #ddd; padding-bottom: 6px">{{ section.label }}（共 {{ section.questions.length }} 题）</h2>
          <div v-for="(q, localIdx) in section.questions" :key="q.questionId" style="margin-bottom: 20px">
            <strong>{{ section.startIndex + localIdx + 1 }}. {{ q.questionContent }}</strong>
            <p v-if="q.optionA" style="margin: 6px 0 0 12px">A. {{ q.optionA }}</p>
            <p v-if="q.optionB" style="margin: 2px 0 0 12px">B. {{ q.optionB }}</p>
            <p v-if="q.optionC" style="margin: 2px 0 0 12px">C. {{ q.optionC }}</p>
            <p v-if="q.optionD" style="margin: 2px 0 0 12px">D. {{ q.optionD }}</p>
            <p style="margin: 4px 0 0 12px; color: #666; font-size: 12px">（{{ q.score }} 分）</p>
          </div>
        </template>
      </div>
      </template>
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
  background: var(--glow-blue);
  color: var(--accent-blue);
}
.q-nav-btn--done {
  background: var(--glow-blue);
  border-color: color-mix(in srgb, var(--accent-blue) 25%, transparent);
  color: var(--accent-blue);
}
.q-nav-btn--active {
  border-color: var(--accent-blue);
  background: var(--accent-blue);
  color: #fff;
  box-shadow: 0 2px 6px rgba(0,0,0,0.18);
}

.section-header-card {
  margin-bottom: 14px;
  background: color-mix(in srgb, var(--el-color-primary) 8%, transparent);
  border-left: 4px solid var(--el-color-primary);
}

.section-header-label {
  font-size: 16px;
  font-weight: 700;
  color: var(--el-color-primary);
}

.judge-option {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  font-size: 16px;
  font-weight: 700;
}
.judge-true {
  color: var(--el-color-success);
  background: color-mix(in srgb, var(--el-color-success) 12%, transparent);
}
.judge-false {
  color: var(--el-color-danger);
  background: color-mix(in srgb, var(--el-color-danger) 12%, transparent);
}

.nav-section-label {
  font-size: 11px;
  font-weight: 700;
  color: var(--el-color-primary);
  margin-bottom: 4px;
  margin-top: 4px;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}
</style>
