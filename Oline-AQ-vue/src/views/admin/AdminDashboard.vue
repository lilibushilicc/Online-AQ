<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useExamStore } from '@/stores/exam'
import { loadQuestionsApi, getDashboardStatsApi } from '@/api'
import CanvasDoughnut from '@/views/components/CanvasDoughnut.vue'
import CanvasRadialGauge from '@/views/components/CanvasRadialGauge.vue'

const store = useExamStore()
const loading = ref(true)
const questionCount = ref(0)
const stats = ref<{ scoreDistribution: Record<string, number>; passRate: number; hardestQuestions: { questionId: number; questionContent: string; correctRate: number; totalAnswers: number }[] } | null>(null)
const latestExams = computed(() => store.exams.slice(0, 4))
const publishedRate = computed(() => {
  if (store.exams.length === 0) return 0
  return Math.round((store.publishedExams.length / store.exams.length) * 100)
})
const closedCount = computed(() => store.exams.filter((exam) => exam.status === 'closed').length)
const draftCount = computed(() => store.exams.filter((exam) => exam.status === 'draft').length)
const totalScore = computed(() => store.exams.reduce((sum, exam) => sum + Number(exam.totalScore), 0))
const draftRate = computed(() => store.exams.length ? Math.round((draftCount.value / store.exams.length) * 100) : 0)
const closedRate = computed(() => store.exams.length ? Math.round((closedCount.value / store.exams.length) * 100) : 0)
const averageScore = computed(() => store.averageScore)
const totalDuration = computed(() => store.exams.reduce((sum, exam) => sum + (exam.duration || 0), 0))

const scoreLabels: Record<string, string> = { range0_20: '0-20', range21_40: '21-40', range41_60: '41-60', range61_80: '61-80', range81_100: '81-100' }
const scoreColors: Record<string, string> = { range0_20: '#ef4444', range21_40: '#f59e0b', range41_60: '#f97316', range61_80: '#3b82f6', range81_100: '#10b981' }
const doughnutSegments = computed(() => {
  if (!stats.value) return []
  return Object.entries(scoreLabels).map(([key, label]) => ({
    label,
    value: stats.value!.scoreDistribution[key] ?? 0,
    color: scoreColors[key] ?? '#94a3b8',
  }))
})

const cells = computed(() => [
  { label: '题库题目', value: questionCount.value, unit: '道', desc: '可用于组卷的有效题量' },
  { label: '考试数量', value: store.exams.length, unit: '场', desc: '草稿、已发布、已关闭合计' },
  { label: '发布率', value: publishedRate.value, unit: '%', desc: `${store.publishedExams.length} 场正在开放` },
  { label: '平均得分', value: averageScore.value, unit: '分', desc: '来自当前成绩记录' },
])

onMounted(async () => {
  try {
    const [qResult, , statsData] = await Promise.all([
      loadQuestionsApi(undefined, 1, 1),
      store.loadExams(),
      getDashboardStatsApi(),
    ])
    questionCount.value = qResult.total ?? 0
    stats.value = statsData
  } catch {
    ElMessage.error('加载数据失败，请刷新重试')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div v-loading="loading" class="bento-grid">
    <!-- 数据卡片 -->
    <div
      v-for="(cell, index) in cells"
      :key="cell.label"
      class="bento-cell fade-in"
      :class="[`stagger-${index + 1}`]"
    >
      <div class="bento-label">{{ cell.label }}</div>
      <div class="bento-value">
        {{ cell.value }}
        <span class="bento-value__unit">{{ cell.unit }}</span>
      </div>
      <div class="bento-trend">{{ cell.desc }}</div>
    </div>

    <!-- 测验资产总览 -->
    <div class="bento-cell bento-cell-wide fade-in stagger-5">
      <div class="bento-label">测验资产总览</div>
      <div class="asset-bars">
        <div class="asset-bar">
          <span class="asset-bar__label">已发布</span>
          <el-progress :percentage="publishedRate" :stroke-width="8" :show-text="false" color="var(--accent-blue)" />
          <strong class="asset-bar__value">{{ publishedRate }}%</strong>
        </div>
        <div class="asset-bar">
          <span class="asset-bar__label">草稿</span>
          <el-progress :percentage="draftRate" :stroke-width="8" :show-text="false" color="var(--accent-amber)" />
          <strong class="asset-bar__value">{{ draftCount }}</strong>
        </div>
        <div class="asset-bar">
          <span class="asset-bar__label">已关闭</span>
          <el-progress :percentage="closedRate" :stroke-width="8" :show-text="false" color="#94a3b8" />
          <strong class="asset-bar__value">{{ closedCount }}</strong>
        </div>
      </div>
      <div class="bento-trend asset-summary">
        累计设计分值 <strong>{{ totalScore }}</strong> 分 | 总计时长 <strong>{{ totalDuration }}</strong> 分钟
      </div>
    </div>

    <!-- 分数分布 -->
    <div class="bento-cell bento-cell-wide fade-in stagger-6" v-if="stats">
      <div class="bento-label">分数分布</div>
      <div class="score-dist-canvas">
        <CanvasDoughnut :segments="doughnutSegments" :size="160" :innerRadius="48" />
        <div class="score-legend">
          <div v-for="seg in doughnutSegments" :key="seg.label" class="score-legend__item">
            <span class="score-legend__dot" :style="{ background: seg.color }" />
            <span class="score-legend__label">{{ seg.label }}</span>
            <span class="score-legend__value">{{ seg.value }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 通过率 -->
    <div class="bento-cell pass-rate-cell fade-in stagger-7" v-if="stats">
      <div class="bento-label">通过率</div>
      <div class="pass-rate-gauge-wrap">
        <CanvasRadialGauge
          :value="stats.passRate"
          :size="130"
          :strokeWidth="10"
          color="#10b981"
          label="通过率"
        />
      </div>
      <div class="bento-trend">得分大于 0 视为通过</div>
    </div>

    <div class="bento-cell bento-cell-wide fade-in stagger-8" v-if="stats && stats.hardestQuestions.length > 0">
      <div class="bento-label">最难题目 Top 5</div>
      <div class="hardest-list">
        <div v-for="(q, i) in stats.hardestQuestions" :key="q.questionId" class="hardest-item">
          <span class="hardest-rank">#{{ i + 1 }}</span>
          <span class="hardest-content">{{ q.questionContent }}</span>
          <el-tag :type="q.correctRate < 30 ? 'danger' : 'warning'" size="small">{{ q.correctRate }}%</el-tag>
        </div>
      </div>
    </div>

    <!-- 快捷操作 -->
    <div class="bento-cell fade-in stagger-9 quick-actions-cell">
      <div class="bento-label">快捷操作</div>
      <div class="quick-actions">
        <div class="quick-action-item" @click="$router.push('/admin/upload')">
          <div class="qa-icon qa-icon--blue">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
          </div>
          <div>
            <div class="qa-label">上传试题</div>
            <div class="qa-desc">TXT / DOCX 解析入库</div>
          </div>
        </div>
        <div class="quick-action-item" @click="$router.push('/admin/exams')">
          <div class="qa-icon qa-icon--green">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
          </div>
          <div>
            <div class="qa-label">创建考试</div>
            <div class="qa-desc">组卷并发布</div>
          </div>
        </div>
        <div class="quick-action-item" @click="$router.push('/admin/results')">
          <div class="qa-icon qa-icon--amber">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18"><path d="M22 12h-4l-3 9L9 3l-3 9H2"/></svg>
          </div>
          <div>
            <div class="qa-label">查看成绩</div>
            <div class="qa-desc">统计与详情</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 工作流程 -->
    <div class="bento-cell bento-cell-full fade-in stagger-10">
      <div class="bento-section-header">
        <span class="bento-section-title">工作流程</span>
      </div>
      <div class="workflow-steps">
        <el-steps :active="5" align-center>
          <el-step title="上传" description="TXT / DOCX" />
          <el-step title="解析" description="题干与答案" />
          <el-step title="组卷" description="选择题目" />
          <el-step title="发布" description="学生端可见" />
          <el-step title="评分" description="自动生成成绩" />
        </el-steps>
      </div>
    </div>

    <!-- 最近考试 -->
    <div class="bento-cell bento-cell-full fade-in stagger-11 recent-exams-cell">
      <div class="bento-section-header">
        <span class="bento-section-title">最近考试</span>
        <el-button link type="primary" size="small" @click="$router.push('/admin/exams')">管理考试</el-button>
      </div>
      <el-table :data="latestExams" stripe>
        <el-table-column prop="examName" label="考试名称" min-width="180" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'published' ? 'success' : row.status === 'closed' ? 'info' : 'warning'" size="small" effect="plain">
              {{ row.status === 'published' ? '已发布' : row.status === 'closed' ? '已关闭' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="duration" label="时长" width="70" />
        <el-table-column prop="totalScore" label="总分" width="70" />
      </el-table>
    </div>
  </div>
</template>

<style scoped>
.bento-value__unit {
  font-size: var(--text-card-title);
  font-weight: 400;
  color: var(--text-tertiary);
  letter-spacing: 0;
}

.asset-bars {
  display: grid;
  gap: 12px;
  margin-top: 4px;
}

.asset-bar {
  display: grid;
  grid-template-columns: 64px 1fr 40px;
  gap: 10px;
  align-items: center;
}

.asset-bar__label {
  color: var(--text-tertiary);
  font-size: var(--text-small);
}

.asset-bar__value {
  font-size: var(--text-caption);
}

.asset-summary {
  margin-top: 10px;
}

.quick-actions-cell {
  display: flex;
  flex-direction: column;
}

.bento-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid var(--border-subtle);
}

.bento-section-title {
  font-size: var(--text-label);
  font-weight: 600;
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.workflow-steps {
  padding: 16px 20px;
}

.recent-exams-cell {
  padding: 0;
}

.score-dist-canvas {
  display: flex;
  align-items: center;
  gap: 28px;
  margin-top: 4px;
}

.score-legend {
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: 1;
}

.score-legend__item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 10px;
  border-radius: var(--radius-sm);
  transition: background var(--duration-fast);
}
.score-legend__item:hover {
  background: var(--sidebar-hover);
}

.score-legend__dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.score-legend__label {
  font-size: var(--text-caption);
  color: var(--text-secondary);
  flex: 1;
}

.score-legend__value {
  font-size: var(--text-caption);
  font-weight: 700;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}

.pass-rate-cell {
  display: flex;
  flex-direction: column;
}

.pass-rate-gauge-wrap {
  display: flex;
  justify-content: center;
  padding: 10px 0;
}

.hardest-list {
  display: grid;
  gap: 8px;
  margin-top: 8px;
}

.hardest-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.hardest-rank {
  font-size: var(--text-small);
  font-weight: 600;
  color: var(--text-tertiary);
  min-width: 24px;
}

.hardest-content {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: var(--text-caption);
  color: var(--text-secondary);
}

.qa-icon--blue { background: var(--glow-blue); color: var(--accent-blue); }
.qa-icon--green { background: rgba(5,150,105,0.08); color: var(--accent-green); }
.qa-icon--amber { background: rgba(217,119,6,0.08); color: var(--accent-amber); }

[data-theme="dark"] .qa-icon--blue { background: rgba(45,212,191,0.15); }
[data-theme="dark"] .qa-icon--green { background: rgba(52,211,153,0.12); }
[data-theme="dark"] .qa-icon--amber { background: rgba(251,191,36,0.12); }
</style>
