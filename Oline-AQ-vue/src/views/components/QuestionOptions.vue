<script setup lang="ts">
defineProps<{
  question: {
    questionType: string
    optionA: string
    optionB: string
    optionC?: string
    optionD?: string
  }
  correctAnswer?: string
}>()
</script>

<template>
  <template v-if="question.questionType === 'single' || question.questionType === 'judge'">
    <div class="option-grid">
      <span :class="correctAnswer === 'A' ? 'correct-option' : ''">A. {{ question.optionA }}</span>
      <span :class="correctAnswer === 'B' ? 'correct-option' : ''">B. {{ question.optionB }}</span>
      <span v-if="question.optionC" :class="correctAnswer === 'C' ? 'correct-option' : ''">C. {{ question.optionC }}</span>
      <span v-if="question.optionD" :class="correctAnswer === 'D' ? 'correct-option' : ''">D. {{ question.optionD }}</span>
    </div>
  </template>
</template>

<style scoped>
.option-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px 24px;
  margin: 8px 0;
  font-size: 14px;
  color: var(--text-secondary);
}
.correct-option {
  color: var(--accent-green);
  font-weight: 600;
}
</style>
