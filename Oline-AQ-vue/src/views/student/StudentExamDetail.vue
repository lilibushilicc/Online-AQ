<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import StudentLayout from './StudentLayout.vue'
import { useExamStore, type Exam, type Question } from '@/stores/exam'

const route = useRoute()
const router = useRouter()
const store = useExamStore()
const examId = Number(route.params.examId)
const answers = ref<Record<number, string>>({})
const exam = ref<Exam | null>(null)
const questions = ref<Question[]>([])
const remainingSeconds = ref(0)
const initialRemainingSeconds = ref(0)
const submitting = ref(false)
const activeQuestionIndex = ref(0)
let timer: number | null = null

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
  return ''
}

function resolveRemainingSeconds(currentExam: Exam) {
  const durationLimit = Math.max(0, (currentExam.duration || 0) * 60)
  const endLimit = currentExam.endTime
    ? Math.max(0, Math.floor((new Date(currentExam.endTime).getTime() - Date.now()) / 1000))
    : durationLimit
  if (durationLimit === 0) return endLimit
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
  if (blockedReason && !autoSubmit) { ElMessage.warning(blockedReason); return }
  if (!autoSubmit && answeredCount.value < questions.value.length) { ElMessage.warning('请完成所有题目后再提交'); return }
  if (!autoSubmit) await ElMessageBox.confirm('提交后将立即自动评分，确认现在提交吗？', '提交试卷', { type: 'warning' })
  submitting.value = true
  clearTimer()
  try {
    const result = await store.submitExam(exam.value.examId, answers.value, elapsedSeconds.value)
    if (autoSubmit) ElMessage.warning('考试时间已到，系统已自动交卷')
    else ElMessage.success(`提交成功，得分 ${result.totalScore}`)
    router.push(`/student/results/${result.resultId}`)
  } finally { submitting.value = false }
}

function startCountdown(currentExam: Exam) {
  initialRemainingSeconds.value = resolveRemainingSeconds(currentExam)
  remainingSeconds.value = initialRemainingSeconds.value
  if (remainingSeconds.value <= 0) { void handleSubmit({ auto: true }); return }
  clearTimer()
  timer = window.setInterval(() => {
    if (remainingSeconds.value <= 1) { remainingSeconds.value = 0; clearTimer(); void handleSubmit({ auto: true }); return }
    remainingSeconds.value -= 1
  }, 1000)
}

onMounted(async () => {
  try {
    await store.loadMyResults()
    const detail = await store.getExamDetail(examId)
    exam.value = detail.exam
    questions.value = detail.questions
    if (!exam.value) return
    const blockedReason = examBlockedReason(exam.value)
    if (blockedReason) { ElMessage.warning(blockedReason); router.replace('/student/exams'); return }
    startCountdown(exam.value)
    window.addEventListener('scroll', updateActiveQuestion, { passive: true })
  } catch {
    ElMessage.error('加载试卷失败，请返回重试')
  }
})

onUnmounted(() => {
  clearTimer()
  window.removeEventListener('scroll', updateActiveQuestion)
})
</script>

<template>
  <StudentLayout :title="exam?.examName ?? '考试加载中'" :subtitle="exam ? `时长 ${exam.duration} 分钟，总分 ${exam.totalScore} 分` : ''">
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
          :body-style="index === activeQuestionIndex ? { borderLeft: '3px solid var(--ink-green)' } : {}"
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
          <div style="font-size: 12px; font-weight: 600; color: var(--muted); text-transform: uppercase; letter-spacing: 0.4px; margin-bottom: 8px">题目导航</div>
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
        <el-button type="primary" size="large" style="width: 100%" :loading="submitting" @click="handleSubmit()">
          提交试卷
        </el-button>
      </aside>
    </section>
    <section v-else>
      <el-empty description="考试不存在或正在加载" />
    </section>
  </StudentLayout>
</template>

<style scoped>
.q-nav-btn {
  width: 32px;
  height: 32px;
  border-radius: 4px;
  border: 1px solid var(--line);
  background: var(--paper);
  color: var(--ink-secondary);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--duration-fast);
  display: flex;
  align-items: center;
  justify-content: center;
}
.q-nav-btn:hover {
  border-color: var(--ink-green-light);
  background: var(--accent-light);
  color: var(--ink-green);
}
.q-nav-btn--done {
  background: var(--accent-light);
  border-color: rgba(45,90,71,0.25);
  color: var(--ink-green);
}
.q-nav-btn--active {
  border-color: var(--ink-green);
  background: var(--ink-green);
  color: #fff;
  box-shadow: 0 2px 6px rgba(45,90,71,0.25);
}
</style>