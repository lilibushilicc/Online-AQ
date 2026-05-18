<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import StudentLayout from './StudentLayout.vue'
import { useExamStore, type Exam, type Question } from '@/stores/exam'

const route = useRoute()
const router = useRouter()
const store = useExamStore()
const examId = Number(route.params.examId)
const answers = reactive<Record<number, string>>({})
const exam = ref<Exam | null>(null)
const questions = ref<Question[]>([])
const remainingSeconds = ref(0)
const initialRemainingSeconds = ref(0)
const submitting = ref(false)
let timer: number | null = null

const answeredCount = computed(() => Object.keys(answers).length)
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

    if (start !== null && now < start) {
      return '考试尚未开始'
    }
    if (end !== null && now > end) {
      return '考试已结束'
    }
    return '考试当前不可作答'
  }

  if (!currentExam.allowRetake && store.hasSubmittedExam(currentExam.examId)) {
    return '该考试仅允许提交一次'
  }

  return ''
}

function resolveRemainingSeconds(currentExam: Exam) {
  const durationLimit = Math.max(0, (currentExam.duration || 0) * 60)
  const endLimit = currentExam.endTime
    ? Math.max(0, Math.floor((new Date(currentExam.endTime).getTime() - Date.now()) / 1000))
    : durationLimit

  if (durationLimit === 0) {
    return endLimit
  }
  if (!currentExam.endTime) {
    return durationLimit
  }
  return Math.min(durationLimit, endLimit)
}

function clearTimer() {
  if (timer !== null) {
    window.clearInterval(timer)
    timer = null
  }
}

async function handleSubmit(options?: { auto?: boolean }) {
  if (!exam.value || submitting.value) {
    return
  }

  const autoSubmit = Boolean(options?.auto)
  const blockedReason = examBlockedReason(exam.value)
  if (blockedReason && !autoSubmit) {
    ElMessage.warning(blockedReason)
    return
  }

  if (!autoSubmit && answeredCount.value < questions.value.length) {
    ElMessage.warning('请完成所有题目后再提交')
    return
  }

  if (!autoSubmit) {
    await ElMessageBox.confirm('提交后将立即自动评分，确认现在提交吗？', '提交试卷', { type: 'warning' })
  }

  submitting.value = true
  clearTimer()

  try {
    const result = await store.submitExam(exam.value.examId, answers, elapsedSeconds.value)
    if (autoSubmit) {
      ElMessage.warning('考试时间已到，系统已自动交卷')
    } else {
      ElMessage.success(`提交成功，得分 ${result.totalScore}`)
    }
    router.push(`/student/results/${result.resultId}`)
  } finally {
    submitting.value = false
  }
}

function startCountdown(currentExam: Exam) {
  initialRemainingSeconds.value = resolveRemainingSeconds(currentExam)
  remainingSeconds.value = initialRemainingSeconds.value

  if (remainingSeconds.value <= 0) {
    void handleSubmit({ auto: true })
    return
  }

  clearTimer()
  timer = window.setInterval(() => {
    if (remainingSeconds.value <= 1) {
      remainingSeconds.value = 0
      clearTimer()
      void handleSubmit({ auto: true })
      return
    }
    remainingSeconds.value -= 1
  }, 1000)
}

onMounted(async () => {
  await store.loadMyResults()
  const detail = await store.getExamDetail(examId)
  exam.value = detail.exam
  questions.value = detail.questions

  if (!exam.value) {
    return
  }

  const blockedReason = examBlockedReason(exam.value)
  if (blockedReason) {
    ElMessage.warning(blockedReason)
    router.replace('/student/exams')
    return
  }

  startCountdown(exam.value)
})

onUnmounted(() => {
  clearTimer()
})
</script>

<template>
  <StudentLayout :title="exam?.examName ?? '考试加载中'" :subtitle="exam ? `时长 ${exam.duration} 分钟，总分 ${exam.totalScore} 分` : ''">
    <section v-if="exam" class="answer-layout">
      <div class="question-list">
        <article class="question-card">
          <div class="status-line">
            <div class="status-box"><span class="muted">开放开始</span><b>{{ exam.startTime ? new Date(exam.startTime).toLocaleString() : '未设置' }}</b></div>
            <div class="status-box"><span class="muted">开放结束</span><b>{{ exam.endTime ? new Date(exam.endTime).toLocaleString() : '未设置' }}</b></div>
            <div class="status-box"><span class="muted">历史提交</span><b>{{ historyCount }} 次</b></div>
          </div>
        </article>

        <article v-for="(question, index) in questions" :key="question.questionId" class="question-card">
          <div class="toolbar" style="margin-bottom: 10px">
            <h3>{{ index + 1 }}. {{ question.questionContent }}</h3>
            <el-tag>{{ question.score }} 分</el-tag>
          </div>
          <el-radio-group v-model="answers[question.questionId]" class="option-grid">
            <el-radio-button value="A">A. {{ question.optionA }}</el-radio-button>
            <el-radio-button value="B">B. {{ question.optionB }}</el-radio-button>
            <el-radio-button v-if="question.optionC" value="C">C. {{ question.optionC }}</el-radio-button>
            <el-radio-button v-if="question.optionD" value="D">D. {{ question.optionD }}</el-radio-button>
          </el-radio-group>
        </article>
      </div>
      <aside class="answer-index">
        <strong>答题进度</strong>
        <el-progress :percentage="progress" style="margin: 12px 0" />
        <div class="answer-summary">
          <div><span class="muted">已答</span><b>{{ answeredCount }}</b></div>
          <div><span class="muted">剩余</span><b>{{ questions.length - answeredCount }}</b></div>
        </div>
        <div class="status-box">
          <span class="muted">剩余时间</span>
          <b>{{ countdownText }}</b>
        </div>
        <div class="status-box" style="margin-top: 10px">
          <span class="muted">已用时间</span>
          <b>{{ elapsedSeconds }} 秒</b>
        </div>
        <div class="index-grid">
          <span v-for="(question, index) in questions" :key="question.questionId" class="index-dot" :class="{ done: answers[question.questionId] }">
            {{ index + 1 }}
          </span>
        </div>
        <el-button type="primary" size="large" style="width: 100%; margin-top: 18px" :loading="submitting" @click="handleSubmit()">
          提交试卷
        </el-button>
      </aside>
    </section>
    <section v-else class="panel">
      <el-empty description="考试不存在或正在加载" />
    </section>
  </StudentLayout>
</template>
