<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import StudentLayout from './StudentLayout.vue'
import { useExamStore } from '@/stores/exam'

const store = useExamStore()

const answers = ref<Record<number, string>>({})
const submitted = ref(false)
const score = ref(0)
const practiceCategory = ref('')

const categoryOptions = computed(() => {
  const cats = new Set(store.questions.map((q) => q.category).filter(Boolean))
  return Array.from(cats) as string[]
})
const questions = computed(() => {
  if (!practiceCategory.value) return store.questions
  return store.questions.filter((q) => q.category === practiceCategory.value)
})
const answeredCount = computed(() => Object.keys(answers.value).length)
const progress = computed(() =>
  questions.value.length === 0 ? 0 : Math.round((answeredCount.value / questions.value.length) * 100),
)

function handleSubmit() {
  if (answeredCount.value < questions.value.length) {
    ElMessage.warning('请完成所有题目后再提交')
    return
  }

  let correctCount = 0
  for (const q of questions.value) {
    if (answers.value[q.questionId] === q.correctAnswer) {
      correctCount++
    }
  }
  score.value = correctCount
  submitted.value = true
  ElMessage.success(`练习完成！正确 ${correctCount} / ${questions.value.length}`)
}

function handleRetry() {
  answers.value = {}
  submitted.value = false
  score.value = 0
}

function isCorrect(questionId: number) {
  return answers.value[questionId] === questions.value.find((q) => q.questionId === questionId)?.correctAnswer
}

function getOptionText(question: { optionA: string; optionB: string; optionC?: string; optionD?: string }, letter: string) {
  const key = `option${letter}` as 'optionA' | 'optionB' | 'optionC' | 'optionD'
  return question[key] ?? ''
}

function handleCategoryChange() {
  answers.value = {}
  submitted.value = false
  score.value = 0
}

onMounted(async () => {
  await Promise.all([store.loadQuestions(), store.loadCategories()])
})
</script>

<template>
  <StudentLayout title="在线做题" subtitle="题库练习，所有题目自由作答，即时反馈">
    <section v-if="store.questions.length > 0">
      <div style="margin-bottom: 16px">
        <el-select v-model="practiceCategory" placeholder="按分类练习" clearable style="width: 200px" @change="handleCategoryChange">
          <el-option v-for="item in categoryOptions" :key="item" :label="item" :value="item" />
        </el-select>
        <span class="muted" style="margin-left: 12px; font-size: 13px">显示 {{ questions.length }} / {{ store.questions.length }} 道题</span>
      </div>
      <el-row :gutter="14" style="margin-bottom: 16px">
        <el-col :xs="24" :sm="12" :lg="6">
          <el-card shadow="hover">
            <el-statistic title="题目总数" :value="questions.length">
              <template #suffix><span style="font-size: 14px; color: var(--muted)">题</span></template>
            </el-statistic>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :lg="6">
          <el-card shadow="hover">
            <el-statistic title="已作答" :value="answeredCount">
              <template #suffix><span style="font-size: 14px; color: var(--muted)">题</span></template>
            </el-statistic>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :lg="6">
          <el-card shadow="hover">
            <el-statistic title="完成进度" :value="`${progress}%`" />
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :lg="6">
          <el-card shadow="hover">
            <el-statistic v-if="submitted" title="正确数量" :value="score">
              <template #suffix><span style="font-size: 14px; color: var(--muted)">题</span></template>
            </el-statistic>
            <el-statistic v-else title="状态" value="作答中" />
          </el-card>
        </el-col>
      </el-row>

      <el-card v-for="(question, index) in questions" :key="question.questionId" shadow="hover" style="margin-bottom: 14px">
        <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px">
          <strong>{{ index + 1 }}. {{ question.questionContent }}</strong>
          <div style="display: flex; gap: 8px; align-items: center">
            <el-tag size="small">{{ question.score }} 分</el-tag>
            <el-tag v-if="submitted" :type="isCorrect(question.questionId) ? 'success' : 'danger'" size="small">
              {{ isCorrect(question.questionId) ? '正确' : '错误' }}
            </el-tag>
          </div>
        </div>
        <el-radio-group v-model="answers[question.questionId]" :disabled="submitted">
          <el-radio value="A">A. {{ question.optionA }}</el-radio>
          <el-radio value="B">B. {{ question.optionB }}</el-radio>
          <el-radio v-if="question.optionC" value="C">C. {{ question.optionC }}</el-radio>
          <el-radio v-if="question.optionD" value="D">D. {{ question.optionD }}</el-radio>
        </el-radio-group>
        <div v-if="submitted && !isCorrect(question.questionId)" style="margin-top: 10px; color: var(--el-color-success)">
          正确答案：{{ question.correctAnswer }}. {{ getOptionText(question, question.correctAnswer) }}
        </div>
      </el-card>

      <div style="display: flex; justify-content: center; gap: 14px; margin-top: 20px">
        <el-button v-if="submitted" type="primary" size="large" @click="handleRetry">重新练习</el-button>
        <el-button v-else type="primary" size="large" :disabled="answeredCount < questions.length" @click="handleSubmit">
          提交答案
        </el-button>
      </div>
    </section>
    <el-empty v-if="store.questions.length === 0" description="题库暂无题目，请联系管理员上传题目" />
    <el-empty v-else description="当前分类下暂无题目，请选择其他分类" />
  </StudentLayout>
</template>
