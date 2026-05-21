<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import StatCards from '@/views/components/StatCards.vue'
import { useExamStore } from '@/stores/exam'

const store = useExamStore()

const answers = ref<Record<number, string>>({})
const submitted = ref(false)
const score = ref(0)
const practiceCategory = ref('')
const submitting = ref(false)

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

async function handleSubmit() {
  if (answeredCount.value < questions.value.length) {
    ElMessage.warning('请完成所有题目后再提交')
    return
  }
  submitting.value = true
  let correctCount = 0
  for (const q of questions.value) {
    if (answers.value[q.questionId] === q.correctAnswer) {
      correctCount++
    }
  }
  score.value = correctCount
  submitted.value = true
  submitting.value = false
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
  try {
    await Promise.all([store.loadQuestions(), store.loadCategories()])
  } catch {
    ElMessage.error('加载题库失败，请刷新重试')
  }
})
</script>

<template>
  <section v-if="store.questions.length > 0" class="practice-page">
    <div class="practice-toolbar">
      <el-select v-model="practiceCategory" placeholder="按分类练习" clearable class="practice-category-select" @change="handleCategoryChange">
        <el-option v-for="item in categoryOptions" :key="item" :label="item" :value="item" />
      </el-select>
      <span class="muted">显示 {{ questions.length }} / {{ store.questions.length }} 道题</span>
    </div>

    <StatCards :items="[
      { title: '题目总数', value: questions.length, suffix: '题' },
      { title: '已作答', value: answeredCount, suffix: '题' },
      { title: '完成进度', value: `${progress}%` },
      ...(submitted
        ? [{ title: '正确数量', value: score, suffix: '题' as string }]
        : [{ title: '状态', value: '作答中' as string }]
      ),
    ]" />

    <div class="practice-questions">
      <el-card v-for="(question, index) in questions" :key="question.questionId" shadow="never" class="practice-card">
        <div class="practice-card__header">
          <strong class="practice-card__title">{{ index + 1 }}. {{ question.questionContent }}</strong>
          <div class="practice-card__tags">
            <el-tag size="small" effect="plain">{{ question.score }} 分</el-tag>
            <el-tag v-if="submitted" :type="isCorrect(question.questionId) ? 'success' : 'danger'" size="small" effect="plain">
              {{ isCorrect(question.questionId) ? '正确' : '错误' }}
            </el-tag>
          </div>
        </div>

        <template v-if="question.questionType === 'single' || question.questionType === 'judge'">
          <el-radio-group v-model="answers[question.questionId]" :disabled="submitted" class="practice-options">
            <el-radio value="A">A. {{ question.optionA }}</el-radio>
            <el-radio value="B">B. {{ question.optionB }}</el-radio>
            <el-radio v-if="question.optionC" value="C">C. {{ question.optionC }}</el-radio>
            <el-radio v-if="question.optionD" value="D">D. {{ question.optionD }}</el-radio>
          </el-radio-group>
        </template>
        <template v-else-if="question.questionType === 'fill_blank'">
          <el-input v-model="answers[question.questionId]" placeholder="请输入答案" :disabled="submitted" clearable class="practice-fill-input" />
        </template>
        <template v-else-if="question.questionType === 'short_answer'">
          <el-input v-model="answers[question.questionId]" type="textarea" :rows="4" placeholder="请输入你的回答" :disabled="submitted" />
        </template>

        <div v-if="submitted && !isCorrect(question.questionId) && (question.questionType === 'single' || question.questionType === 'judge')" class="practice-answer-hint">
          正确答案：{{ question.correctAnswer }}. {{ getOptionText(question, question.correctAnswer) }}
        </div>
        <div v-else-if="submitted && !isCorrect(question.questionId)" class="practice-answer-hint">
          参考答案：{{ question.correctAnswer }}
        </div>
      </el-card>
    </div>

    <div class="practice-actions">
      <el-button v-if="submitted" type="primary" size="large" @click="handleRetry">重新练习</el-button>
      <el-button v-else type="primary" size="large" :loading="submitting" :disabled="answeredCount < questions.length" @click="handleSubmit">
        提交答案
      </el-button>
    </div>
  </section>

  <el-empty v-if="store.questions.length === 0" description="题库暂无题目，请联系管理员上传题目" />
  <el-empty v-else-if="questions.length === 0" description="当前分类下暂无题目，请选择其他分类" />
</template>

<style scoped>
.practice-page {
  padding-bottom: 24px;
}

.practice-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.practice-category-select {
  width: 200px;
}

.practice-questions {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.practice-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.practice-card__title {
  font-size: var(--text-card-title);
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.6;
}

.practice-card__tags {
  display: flex;
  gap: 6px;
  align-items: center;
  flex-shrink: 0;
}

.practice-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.practice-fill-input {
  max-width: 400px;
}

.practice-answer-hint {
  margin-top: 12px;
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  background: rgba(16,185,129,0.1);
  color: var(--accent-green);
  font-size: var(--text-caption);
  font-weight: 500;
}

.practice-actions {
  display: flex;
  justify-content: center;
  gap: 14px;
  margin-top: 24px;
}
</style>
