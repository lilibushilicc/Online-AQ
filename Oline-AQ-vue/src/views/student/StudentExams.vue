<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import StatCards from '@/views/components/StatCards.vue'
import { useExamStore, type Exam } from '@/stores/exam'
import { formatTime } from '@/utils/format'

const store = useExamStore()
const loading = ref(true)
const searchKeyword = ref('')

function getAvailability(exam: Exam) {
  const now = Date.now()
  const start = exam.startTime ? new Date(exam.startTime).getTime() : null
  const end = exam.endTime ? new Date(exam.endTime).getTime() : null

  if (exam.status !== 'published') {
    return { label: '未发布', type: 'info' as const }
  }
  if (start !== null && now < start) {
    return { label: '未开始', type: 'warning' as const }
  }
  if (end !== null && now > end) {
    return { label: '已结束', type: 'danger' as const }
  }
  return { label: '可参加', type: 'success' as const }
}

function getExamHistory(examId: number) {
  const items = store.results.filter((result) => result.examId === examId)
  items.sort((a, b) => new Date(a.submitTime).getTime() - new Date(b.submitTime).getTime())
  return items
}

function attemptLabel(index: number, total: number) {
  return total > 1 ? `第 ${index + 1} 次` : ''
}

function canStartExam(exam: Exam) {
  if (!store.isExamAccessible(exam)) return false
  if (exam.allowRetake) return true
  return !store.hasSubmittedExam(exam.examId)
}

const availableExams = computed(() => store.publishedExams.filter((e) => canStartExam(e) && (!searchKeyword.value || e.examName.toLowerCase().includes(searchKeyword.value.toLowerCase()))))
const historyExams = computed(() => {
  const submittedIds = [...new Set(store.results.map((r) => r.examId))]
  return submittedIds
    .map((id) => store.exams.find((e) => e.examId === id))
    .filter((exam): exam is Exam => Boolean(exam))
    .filter((exam) => !searchKeyword.value || exam.examName.toLowerCase().includes(searchKeyword.value.toLowerCase()))
})
const filteredPublished = computed(() => store.publishedExams.filter((e) => !searchKeyword.value || e.examName.toLowerCase().includes(searchKeyword.value.toLowerCase())))

onMounted(async () => {
  try {
    await Promise.all([store.loadStudentExams(), store.loadMyResults()])
  } catch {
    ElMessage.error('加载考试列表失败，请刷新重试')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div v-loading="loading" class="exam-page">
    <StatCards :items="[
      { title: '当前可参加', value: availableExams.length, suffix: '场' },
      { title: '已发布考试', value: store.publishedExams.length, suffix: '场' },
      { title: '历史提交', value: store.results.length, suffix: '次' },
      { title: '当前身份', value: store.currentUser?.realName || '学生' },
    ]" />

    <div style="margin-bottom: 16px">
      <el-input v-model="searchKeyword" placeholder="搜索考试名称..." clearable prefix-icon="Search" style="max-width: 320px" />
    </div>

    <!-- 可参加的考试 -->
    <div class="exam-section">
      <div class="exam-section__header">
        <h3 class="exam-section__title">可参加考试</h3>
        <span class="exam-section__count">{{ filteredPublished.length }} 场</span>
      </div>

      <el-row :gutter="16" v-if="filteredPublished.length > 0">
        <el-col v-for="exam in filteredPublished" :key="exam.examId" :xs="24" :md="12" class="exam-col">
          <el-card shadow="never" class="exam-card">
            <template #header>
              <div class="exam-card__header">
                <strong class="exam-card__name">{{ exam.examName }}</strong>
                <el-tag :type="getAvailability(exam).type" size="small" effect="plain">
                  {{ getAvailability(exam).label }}
                </el-tag>
              </div>
            </template>

            <p class="exam-card__desc">{{ exam.description || '暂无考试说明' }}</p>

            <div class="exam-meta">
              <div class="exam-meta__item">
                <span class="exam-meta__label">时长</span>
                <span class="exam-meta__value">{{ exam.duration }} 分钟</span>
              </div>
              <div class="exam-meta__item">
                <span class="exam-meta__label">总分</span>
                <span class="exam-meta__value">{{ exam.totalScore }} 分</span>
              </div>
              <div class="exam-meta__item">
                <span class="exam-meta__label">重考</span>
                <span class="exam-meta__value">{{ exam.allowRetake ? '允许' : '仅一次' }}</span>
              </div>
            </div>

            <div class="exam-meta">
              <div class="exam-meta__item">
                <span class="exam-meta__label">开始</span>
                <span class="exam-meta__value">{{ formatTime(exam.startTime) }}</span>
              </div>
              <div class="exam-meta__item">
                <span class="exam-meta__label">结束</span>
                <span class="exam-meta__value">{{ formatTime(exam.endTime) }}</span>
              </div>
              <div class="exam-meta__item">
                <span class="exam-meta__label">历史</span>
                <span class="exam-meta__value">{{ getExamHistory(exam.examId).length }} 次</span>
              </div>
            </div>

            <div v-if="getExamHistory(exam.examId).length > 0" class="exam-history-tags">
              <el-tag
                v-for="(item, idx) in getExamHistory(exam.examId).slice(-3).reverse()"
                :key="item.resultId"
                type="info"
                effect="plain"
                size="small"
              >
                {{ attemptLabel(idx, getExamHistory(exam.examId).length) }}{{ new Date(item.submitTime).toLocaleString() }} / {{ item.totalScore }} 分
              </el-tag>
            </div>

            <div class="exam-card__footer">
              <span class="exam-card__hint">
                {{ !exam.allowRetake && store.hasSubmittedExam(exam.examId) ? '已提交过，不可再次作答' : '请在开放时间内完成作答' }}
              </span>
              <el-button
                type="primary"
                :disabled="!canStartExam(exam)"
                @click="$router.push(`/student/exams/${exam.examId}`)"
              >
                开始答题
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-empty v-if="filteredPublished.length === 0 && !searchKeyword" description="暂无已发布考试" />
      <el-empty v-if="filteredPublished.length === 0 && searchKeyword" description="没有匹配的考试" />
    </div>

    <!-- 历史试卷 -->
    <div v-if="historyExams.length > 0" class="exam-section">
      <div class="exam-section__header">
        <h3 class="exam-section__title">历史试卷</h3>
        <span class="exam-section__count">{{ historyExams.length }} 场</span>
      </div>

      <el-row :gutter="16">
        <el-col v-for="exam in historyExams" :key="exam.examId" :xs="24" :md="12" class="exam-col">
          <el-card shadow="never" class="exam-card">
            <template #header>
              <div class="exam-card__header">
                <strong class="exam-card__name">{{ exam.examName }}</strong>
                <el-tag :type="exam.status === 'published' ? 'success' : 'info'" size="small" effect="plain">
                  {{ exam.status === 'published' ? '已发布' : '已关闭' }}
                </el-tag>
              </div>
            </template>

            <p class="exam-card__desc">{{ exam.description || '暂无考试说明' }}</p>

            <div class="exam-meta">
              <div class="exam-meta__item">
                <span class="exam-meta__label">时长</span>
                <span class="exam-meta__value">{{ exam.duration }} 分钟</span>
              </div>
              <div class="exam-meta__item">
                <span class="exam-meta__label">总分</span>
                <span class="exam-meta__value">{{ exam.totalScore }} 分</span>
              </div>
              <div class="exam-meta__item">
                <span class="exam-meta__label">提交</span>
                <span class="exam-meta__value">{{ getExamHistory(exam.examId).length }} 次</span>
              </div>
            </div>

            <div class="exam-history-tags">
              <el-tag
                v-for="(item, idx) in getExamHistory(exam.examId)"
                :key="item.resultId"
                type="info"
                effect="plain"
                size="small"
                class="exam-history-tag--clickable"
                @click="$router.push(`/student/results/${item.resultId}`)"
              >
                第 {{ idx + 1 }} 次 · {{ new Date(item.submitTime).toLocaleString() }} / {{ item.totalScore }} 分
              </el-tag>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<style scoped>
.exam-page {
  min-height: 200px;
}

.exam-section {
  margin-top: 28px;
}

.exam-section__header {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 16px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border-subtle);
}

.exam-section__title {
  margin: 0;
  font-size: var(--text-section-title);
  font-weight: 700;
  color: var(--text-primary);
  font-family: var(--font-sans);
}

.exam-section__count {
  font-size: var(--text-small);
  color: var(--text-tertiary);
  font-weight: 500;
}

.exam-col {
  margin-bottom: 16px;
}

.exam-card {
  height: 100%;
}

.exam-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.exam-card__name {
  font-size: var(--text-card-title);
  font-weight: 600;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.exam-card__desc {
  margin: 0 0 16px;
  color: var(--text-tertiary);
  font-size: var(--text-caption);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.exam-meta {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  padding: 10px 0;
  border-top: 1px solid var(--border-subtle);
}

.exam-meta + .exam-meta {
  border-top: none;
  padding-top: 0;
}

.exam-meta__item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.exam-meta__label {
  font-size: var(--text-label);
  color: var(--text-tertiary);
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.exam-meta__value {
  font-size: var(--text-caption);
  color: var(--text-secondary);
  font-weight: 500;
}

.exam-history-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border-subtle);
}

.exam-history-tag--clickable {
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
}

.exam-history-tag--clickable:hover {
  background: rgba(59,130,246,0.1) !important;
  border-color: rgba(45, 90, 71, 0.2) !important;
  color: var(--accent-blue) !important;
}

.exam-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid var(--border-subtle);
}

.exam-card__hint {
  color: var(--text-tertiary);
  font-size: var(--text-small);
  line-height: 1.5;
}
</style>
