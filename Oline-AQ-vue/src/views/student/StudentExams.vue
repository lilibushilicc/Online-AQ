<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Filter, Sort, ArrowRight, User } from '@element-plus/icons-vue'
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
    <!-- Decorative glow elements -->
    <div class="exam-glow-top"></div>
    <div class="exam-glow-bottom"></div>

    <!-- Header Section -->
    <header class="exam-header">
      <div class="exam-header-badge">
        <span class="exam-header-dot"></span>
        <span>学生作答中心</span>
      </div>
      <p class="exam-header-sub">查看当前可用的考试并参与作答</p>
    </header>

    <!-- Stats Grid -->
    <section class="stats-grid">
      <div class="stats-card stats-card--primary">
        <div class="stats-card-label">当前可参加</div>
        <div class="stats-card-row">
          <span class="stats-card-value stats-card-value--accent">{{ availableExams.length }}</span>
          <span class="stats-card-unit">场</span>
        </div>
        <div class="stats-progress">
          <div class="stats-progress-track">
            <div class="stats-progress-fill stats-progress-fill--primary" :style="{ width: filteredPublished.length > 0 ? (availableExams.length / filteredPublished.length * 100) + '%' : '0%' }"></div>
          </div>
        </div>
      </div>
      <div class="stats-card">
        <div class="stats-card-label">已发布考试</div>
        <div class="stats-card-row">
          <span class="stats-card-value">{{ store.publishedExams.length }}</span>
          <span class="stats-card-unit">场</span>
        </div>
        <div class="stats-progress">
          <div class="stats-progress-track">
            <div class="stats-progress-fill stats-progress-fill--secondary" :style="{ width: store.publishedExams.length > 0 ? '50%' : '0%' }"></div>
          </div>
        </div>
      </div>
      <div class="stats-card">
        <div class="stats-card-label">历史提交</div>
        <div class="stats-card-row">
          <span class="stats-card-value">{{ store.results.length }}</span>
          <span class="stats-card-unit">次</span>
        </div>
        <div class="stats-progress">
          <div class="stats-progress-track">
            <div class="stats-progress-fill stats-progress-fill--dim" :style="{ width: store.results.length > 0 ? Math.min(store.results.length / 10 * 100, 100) + '%' : '0%' }"></div>
          </div>
        </div>
      </div>
      <div class="stats-card stats-card--bordered">
        <div class="stats-card-label">当前身份</div>
        <div class="stats-card-id-row">
          <span class="stats-card-id-icon">
            <el-icon :size="22"><User /></el-icon>
          </span>
          <span class="stats-card-id-value">{{ store.currentUser?.realName || '学生' }}</span>
        </div>
        <div class="stats-card-session">Active Session</div>
      </div>
    </section>

    <!-- Search & Filter -->
    <div class="exam-toolbar">
      <div class="exam-search">
        <el-icon class="exam-search-icon"><Search /></el-icon>
        <input v-model="searchKeyword" class="exam-search-input" placeholder="搜索考试名称..." />
      </div>
      <div class="exam-toolbar-actions">
        <button class="exam-toolbar-btn" title="筛选">
          <el-icon><Filter /></el-icon>
        </button>
        <button class="exam-toolbar-btn" title="排序">
          <el-icon><Sort /></el-icon>
        </button>
      </div>
    </div>

    <!-- Available Exams Section -->
    <section class="exam-section">
      <div class="exam-section-header">
        <h3 class="exam-section-title">
          参加考试
          <span class="exam-section-count">{{ filteredPublished.length }}场</span>
        </h3>
      </div>

      <div v-if="filteredPublished.length > 0" class="exam-grid">
        <article v-for="exam in filteredPublished" :key="exam.examId" class="exam-card" :class="{ 'exam-card--ended': getAvailability(exam).type === 'danger' }">
          <div class="exam-card-header">
            <div>
              <h4 class="exam-card-name">{{ exam.examName }}</h4>
              <p class="exam-card-desc">{{ exam.description || '暂无考试说明' }}</p>
            </div>
            <span class="exam-status" :class="'exam-status--' + getAvailability(exam).type">
              {{ getAvailability(exam).label }}
            </span>
          </div>

          <div class="exam-card-meta">
            <div class="exam-meta-item">
              <span class="exam-meta-label">时长</span>
              <span class="exam-meta-value">{{ exam.duration }} 分钟</span>
            </div>
            <div class="exam-meta-item exam-meta-item--bordered">
              <span class="exam-meta-label">总分</span>
              <span class="exam-meta-value">{{ exam.totalScore }} 分</span>
            </div>
            <div class="exam-meta-item">
              <span class="exam-meta-label">重考</span>
              <span class="exam-meta-value">{{ exam.allowRetake ? '允许' : '仅一次' }}</span>
            </div>
          </div>

          <div class="exam-card-info">
            <div class="exam-info-row">
              <span class="exam-info-text">开始 {{ formatTime(exam.startTime) }}</span>
              <span class="exam-info-text">历史: {{ getExamHistory(exam.examId).length }} 次</span>
            </div>
            <div class="exam-info-row exam-info-row--end">
              <span class="exam-info-text">结束: {{ formatTime(exam.endTime) }}</span>
            </div>
          </div>

          <div v-if="getExamHistory(exam.examId).length > 0" class="exam-card-history">
            <span v-for="(item, idx) in getExamHistory(exam.examId).slice(-3).reverse()" :key="item.resultId" class="exam-history-chip" @click="$router.push('/student/results/' + item.resultId)">
              第{{ idx + 1 }}次 · {{ new Date(item.submitTime).toLocaleString() }} / {{ item.totalScore }}分
            </span>
          </div>

          <div class="exam-card-footer">
            <span class="exam-card-hint">
              {{ getAvailability(exam).type === 'danger' ? '作答时间已截止' : !exam.allowRetake && store.hasSubmittedExam(exam.examId) ? '已提交过，不可再次作答' : '请在开放时间内完成作答' }}
            </span>
            <button v-if="getAvailability(exam).type !== 'danger'" class="exam-card-btn" :disabled="!canStartExam(exam)" @click="$router.push('/student/exams/' + exam.examId)">
              <span>开始作答</span>
              <el-icon class="exam-card-btn-icon"><ArrowRight /></el-icon>
            </button>
            <button v-else class="exam-card-btn exam-card-btn--disabled" disabled>
              开始作答
            </button>
          </div>
        </article>
      </div>

      <div v-else class="exam-empty">
        <p>{{ searchKeyword ? '没有匹配的考试' : '暂无已发布考试' }}</p>
      </div>
    </section>

    <!-- History Section -->
    <section v-if="historyExams.length > 0" class="exam-section">
      <div class="exam-section-header">
        <h3 class="exam-section-title">
          历史试卷
          <span class="exam-section-count">{{ historyExams.length }}场</span>
        </h3>
      </div>

      <div class="exam-grid">
        <article v-for="exam in historyExams" :key="exam.examId" class="exam-card">
          <div class="exam-card-header">
            <div>
              <h4 class="exam-card-name">{{ exam.examName }}</h4>
              <p class="exam-card-desc">{{ exam.description || '暂无考试说明' }}</p>
            </div>
            <span class="exam-status" :class="'exam-status--' + (exam.status === 'published' ? 'success' : 'info')">
              {{ exam.status === 'published' ? '已发布' : '已关闭' }}
            </span>
          </div>

          <div class="exam-card-meta">
            <div class="exam-meta-item">
              <span class="exam-meta-label">时长</span>
              <span class="exam-meta-value">{{ exam.duration }} 分钟</span>
            </div>
            <div class="exam-meta-item exam-meta-item--bordered">
              <span class="exam-meta-label">总分</span>
              <span class="exam-meta-value">{{ exam.totalScore }} 分</span>
            </div>
            <div class="exam-meta-item">
              <span class="exam-meta-label">提交</span>
              <span class="exam-meta-value">{{ getExamHistory(exam.examId).length }} 次</span>
            </div>
          </div>

          <div class="exam-card-history">
            <span v-for="(item, idx) in getExamHistory(exam.examId)" :key="item.resultId" class="exam-history-chip exam-history-chip--clickable" @click="$router.push('/student/results/' + item.resultId)">
              第{{ idx + 1 }}次 · {{ new Date(item.submitTime).toLocaleString() }} / {{ item.totalScore }}分
            </span>
          </div>
        </article>
      </div>
    </section>

    <!-- Bottom indicator -->
    <footer class="exam-footer">
      <span class="exam-footer-dot exam-footer-dot--active"></span>
      <span class="exam-footer-dot"></span>
      <span class="exam-footer-dot"></span>
    </footer>
  </div>
</template>

<style scoped>
.exam-page {
  position: relative;
  min-height: 200px;
}

/* Decorative glow */
.exam-glow-top {
  position: fixed;
  top: -80px;
  right: -80px;
  width: 380px;
  height: 380px;
  border-radius: 50%;
  background: rgba(87,241,219,0.03);
  filter: blur(100px);
  pointer-events: none;
  z-index: 0;
}
.exam-glow-bottom {
  position: fixed;
  bottom: 120px;
  left: -80px;
  width: 280px;
  height: 280px;
  border-radius: 50%;
  background: rgba(190,198,224,0.03);
  filter: blur(80px);
  pointer-events: none;
  z-index: 0;
}

/* ===== Header ===== */
.exam-header {
  margin-bottom: 28px;
  position: relative;
  z-index: 1;
}
.exam-header-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 12px;
  border-radius: 999px;
  background: rgba(45,212,191,0.12);
  color: var(--accent-primary);
  font-size: var(--text-small);
  font-weight: 600;
  margin-bottom: 12px;
}
.exam-header-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent-primary);
}
.exam-header-sub {
  margin: 0;
  font-size: 15px;
  color: var(--text-secondary);
}
/* ===== Stats Grid ===== */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 28px;
  position: relative;
  z-index: 1;
}
.stats-card {
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-radius: var(--radius-lg);
  padding: 18px 20px;
  transition: border-color var(--duration-fast) var(--ease-out);
}
.stats-card:hover {
  border-color: rgba(87,241,219,0.2);
}
.stats-card--primary {
  border-left: 3px solid var(--accent-primary);
}
.stats-card--bordered {
  border-left: 3px solid rgba(87,241,219,0.4);
}
.stats-card-label {
  font-size: var(--text-label);
  font-weight: 600;
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  margin-bottom: 6px;
}
.stats-card-row {
  display: flex;
  align-items: baseline;
  gap: 4px;
}
.stats-card-value {
  font-size: 30px;
  font-weight: 700;
  letter-spacing: -0.5px;
  color: var(--text-primary);
  line-height: 1.1;
  font-variant-numeric: tabular-nums;
}
.stats-card-value--accent {
  color: var(--accent-primary);
}
.stats-card-unit {
  font-size: var(--text-body);
  color: var(--text-tertiary);
}
.stats-progress {
  margin-top: 12px;
}
.stats-progress-track {
  width: 100%;
  height: 4px;
  border-radius: 999px;
  background: var(--bg-overlay);
  overflow: hidden;
}
.stats-progress-fill {
  height: 100%;
  border-radius: 999px;
  transition: width 0.6s var(--ease-out);
}
.stats-progress-fill--primary {
  background: var(--accent-primary);
}
.stats-progress-fill--secondary {
  background: var(--secondary);
}
.stats-progress-fill--dim {
  background: var(--text-tertiary);
}
.stats-card-id-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 2px;
}
.stats-card-id-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--accent-primary);
  color: var(--bg-base);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stats-card-id-value {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.3px;
  color: var(--text-primary);
}
.stats-card-session {
  margin-top: 8px;
  font-size: 10px;
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-weight: 600;
}

/* ===== Search & Toolbar ===== */
.exam-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 24px;
  position: relative;
  z-index: 1;
}
.exam-search {
  position: relative;
  display: flex;
  align-items: center;
  flex: 1;
  max-width: 320px;
}
.exam-search-icon {
  position: absolute;
  left: 14px;
  color: var(--text-tertiary);
  font-size: 16px;
  pointer-events: none;
}
.exam-search-input {
  width: 100%;
  padding: 8px 14px 8px 40px;
  background: var(--bg-overlay);
  border: 1px solid var(--border-default);
  border-radius: 999px;
  color: var(--text-primary);
  font-size: 13px;
  font-family: var(--font-sans);
  outline: none;
  transition: border-color var(--duration-fast), box-shadow var(--duration-fast);
}
.exam-search-input:focus {
  border-color: var(--accent-primary);
  box-shadow: 0 0 0 2px rgba(87,241,219,0.1);
}
.exam-search-input::placeholder {
  color: var(--text-disabled);
}
.exam-toolbar-actions {
  display: flex;
  gap: 6px;
}
.exam-toolbar-btn {
  width: 34px;
  height: 34px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-default);
  background: var(--bg-overlay);
  color: var(--text-tertiary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
  font-size: 14px;
}
.exam-toolbar-btn:hover {
  border-color: var(--accent-primary);
  color: var(--accent-primary);
  background: rgba(87,241,219,0.06);
}

/* ===== Section ===== */
.exam-section {
  margin-bottom: 32px;
  position: relative;
  z-index: 1;
}
.exam-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-subtle);
}
.exam-section-title {
  margin: 0;
  font-size: var(--text-section-title);
  font-weight: 700;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 10px;
}
.exam-section-count {
  font-size: var(--text-small);
  font-weight: 500;
  color: var(--text-tertiary);
  background: var(--bg-overlay);
  padding: 1px 10px;
  border-radius: 999px;
}

.exam-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

/* ===== Exam Card ===== */
.exam-card {
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-radius: var(--radius-lg);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  transition: transform var(--duration) var(--ease-out), border-color var(--duration) var(--ease-out);
}
.exam-card:hover {
  transform: scale(1.01);
  border-color: rgba(87,241,219,0.15);
}
.exam-card--ended {
  opacity: 0.6;
}
.exam-card--ended:hover {
  opacity: 1;
  transform: scale(1.01);
}

.exam-card-header {
  padding: 20px 20px 14px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  border-bottom: 1px solid rgba(255,255,255,0.04);
}
.exam-card-name {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.2px;
}
.exam-card-desc {
  margin: 0;
  font-size: var(--text-body);
  color: var(--text-secondary);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.exam-status {
  padding: 3px 12px;
  border-radius: var(--radius-sm);
  font-size: var(--text-small);
  font-weight: 600;
  white-space: nowrap;
  flex-shrink: 0;
}
.exam-status--success {
  background: rgba(87,241,219,0.1);
  color: var(--accent-primary);
  border: 1px solid rgba(87,241,219,0.15);
}
.exam-status--warning {
  background: rgba(251,191,36,0.1);
  color: var(--accent-amber);
  border: 1px solid rgba(251,191,36,0.15);
}
.exam-status--danger {
  background: rgba(255,180,171,0.1);
  color: var(--accent-red);
  border: 1px solid rgba(255,180,171,0.15);
}
.exam-status--info {
  background: rgba(133,148,144,0.1);
  color: var(--text-tertiary);
  border: 1px solid rgba(133,148,144,0.15);
}

.exam-card-meta {
  padding: 14px 20px;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0;
  text-align: center;
}
.exam-meta-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.exam-meta-item--bordered {
  border-left: 1px solid var(--border-subtle);
  border-right: 1px solid var(--border-subtle);
}
.exam-meta-label {
  font-size: 10px;
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  font-weight: 600;
}
.exam-meta-value {
  font-size: var(--text-body);
  color: var(--text-primary);
  font-weight: 600;
}

.exam-card-info {
  padding: 10px 20px;
  background: var(--bg-surface);
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.exam-info-row {
  display: flex;
  justify-content: space-between;
  font-size: var(--text-body);
}
.exam-info-text {
  color: var(--text-secondary);
  font-size: 13px;
}
.exam-info-row--end .exam-info-text {
  color: var(--text-tertiary);
  font-style: italic;
}

.exam-card-history {
  padding: 10px 20px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.exam-history-chip {
  padding: 2px 10px;
  border-radius: 999px;
  background: var(--bg-overlay);
  color: var(--text-tertiary);
  font-size: 11px;
  cursor: default;
  transition: all var(--duration-fast) var(--ease-out);
}
.exam-history-chip--clickable {
  cursor: pointer;
}
.exam-history-chip--clickable:hover {
  background: rgba(87,241,219,0.1);
  color: var(--accent-primary);
}

.exam-card-footer {
  margin-top: auto;
  padding: 14px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-top: 1px solid rgba(255,255,255,0.04);
}
.exam-card-hint {
  font-size: var(--text-small);
  color: var(--text-tertiary);
  font-style: italic;
}

.exam-card-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 24px;
  border: none;
  border-radius: var(--radius-sm);
  background: var(--gradient-primary);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(87,241,219,0.2);
  transition: all var(--duration-fast) var(--ease-out);
  font-family: var(--font-sans);
}
.exam-card-btn:hover:not(:disabled) {
  box-shadow: 0 6px 24px rgba(87,241,219,0.3);
  transform: translateY(-1px);
}
.exam-card-btn:active:not(:disabled) {
  transform: scale(0.97);
}
.exam-card-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.exam-card-btn-icon {
  font-size: 15px;
  transition: transform var(--duration-fast) var(--ease-out);
}
.exam-card-btn:hover:not(:disabled) .exam-card-btn-icon {
  transform: translateX(3px);
}
.exam-card-btn--disabled {
  background: var(--bg-overlay);
  color: var(--text-disabled);
  box-shadow: none;
  cursor: not-allowed;
  opacity: 0.6;
}

/* ===== Empty ===== */
.exam-empty {
  padding: 48px 0;
  text-align: center;
  color: var(--text-tertiary);
  font-size: var(--text-body);
}

/* ===== Footer ===== */
.exam-footer {
  display: flex;
  justify-content: center;
  gap: 6px;
  padding: 32px 0 16px;
}
.exam-footer-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--bg-overlay);
}
.exam-footer-dot--active {
  width: 8px;
  height: 8px;
  background: var(--accent-primary);
}

/* ===== Responsive ===== */
@media (max-width: 1100px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .exam-grid {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 640px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
  .exam-card-header {
    flex-direction: column;
  }
  .exam-toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  .exam-search {
    max-width: none;
  }
}
</style>
