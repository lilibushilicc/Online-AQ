<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useExamStore } from '@/stores/exam'
import { loadQuestionsApi } from '@/api'

const store = useExamStore()
const loading = ref(true)
const questionCount = ref(0)
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

const cells = computed(() => [
  { label: '题库题目', value: questionCount.value, unit: '道', desc: '可用于组卷的有效题量' },
  { label: '考试数量', value: store.exams.length, unit: '场', desc: '草稿、已发布、已关闭合计' },
  { label: '发布率', value: publishedRate.value, unit: '%', desc: `${store.publishedExams.length} 场正在开放` },
  { label: '平均得分', value: averageScore.value, unit: '分', desc: '来自当前成绩记录' },
])

onMounted(async () => {
  try {
    const [qResult] = await Promise.all([loadQuestionsApi(undefined, 1, 1), store.loadExams()])
    questionCount.value = qResult.total ?? 0
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
          <el-progress :percentage="publishedRate" :stroke-width="8" :show-text="false" color="var(--ink-green)" />
          <strong class="asset-bar__value">{{ publishedRate }}%</strong>
        </div>
        <div class="asset-bar">
          <span class="asset-bar__label">草稿</span>
          <el-progress :percentage="draftRate" :stroke-width="8" :show-text="false" color="var(--amber)" />
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

    <!-- 快捷操作 -->
    <div class="bento-cell fade-in stagger-6 quick-actions-cell">
      <div class="bento-label">快捷操作</div>
      <div class="quick-actions">
        <div class="quick-action-item" @click="$router.push('/admin/upload')">
          <div class="qa-icon" style="background: var(--accent-light); color: var(--ink-green)">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
          </div>
          <div>
            <div class="qa-label">上传试题</div>
            <div class="qa-desc">TXT / DOCX 解析入库</div>
          </div>
        </div>
        <div class="quick-action-item" @click="$router.push('/admin/exams')">
          <div class="qa-icon" style="background: var(--green-light); color: var(--green)">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
          </div>
          <div>
            <div class="qa-label">创建考试</div>
            <div class="qa-desc">组卷并发布</div>
          </div>
        </div>
        <div class="quick-action-item" @click="$router.push('/admin/results')">
          <div class="qa-icon" style="background: var(--amber-light); color: var(--amber)">
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
    <div class="bento-cell bento-cell-full fade-in stagger-7">
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
    <div class="bento-cell bento-cell-full fade-in stagger-8 recent-exams-cell">
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
  font-size: 15px;
  font-weight: 400;
  color: var(--muted);
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
  color: var(--muted);
  font-size: 12px;
}

.asset-bar__value {
  font-size: 13px;
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
  border-bottom: 1px solid var(--line-light);
}

.bento-section-title {
  font-size: 11px;
  font-weight: 600;
  color: var(--muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.workflow-steps {
  padding: 16px 20px;
}

.recent-exams-cell {
  padding: 0;
}
</style>
