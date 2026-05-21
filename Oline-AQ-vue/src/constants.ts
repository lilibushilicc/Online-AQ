import type { QuestionType } from '@/types'

export const QUESTION_TYPE_LABEL: Record<QuestionType, string> = {
  single: '单选',
  judge: '判断',
  short_answer: '简答',
  fill_blank: '填空',
}
