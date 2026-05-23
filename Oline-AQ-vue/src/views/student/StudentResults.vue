<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import StatCards from '@/views/components/StatCards.vue'
import { useExamStore } from '@/stores/exam'
import { downloadFile } from '@/utils/download'

const router = useRouter()
const store = useExamStore()
const loading = ref(true)
const searchKeyword = ref('')

const bestScore = computed(() => Math.max(0, ...store.results.map((r) => Number(r.totalScore))))

const filteredGroups = computed(() => examGroups.value.filter((g) => !searchKeyword.value || g.examName.toLowerCase().includes(searchKeyword.value.toLowerCase())))

const examGroups = computed(() => {
  const grouped = new Map<number, { results: typeof store.results; examName: string; allowRetake: boolean }>()
  const examMap = new Map(store.exams.map((e) => [e.examId, e]))

  for (const r of store.results) {
    if (!grouped.has(r.examId)) {
      const exam = examMap.get(r.examId)
      grouped.set(r.examId, {
        results: [],
        examName: exam?.examName ?? `考试 #${r.examId}`,
        allowRetake: exam?.allowRetake ?? false,
      })
    }
    grouped.get(r.examId)!.results.push(r)
  }

  return Array.from(grouped.entries()).map(([examId, group]) => {
    const sorted = group.results.sort(
      (a, b) => new Date(b.submitTime).getTime() - new Date(a.submitTime).getTime(),
    )
    const latest = sorted[0]!
    const best = sorted.reduce(
      (max, r) => (Number(r.totalScore) > Number(max.totalScore) ? r : max),
      latest,
    )
    return {
      examId,
      examName: group.examName,
      allowRetake: group.allowRetake,
      submissionCount: sorted.length,
      latestScore: Number(latest.totalScore),
      bestScore: Number(best.totalScore),
      latestTime: latest.submitTime,
    }
  }).sort((a, b) => new Date(b.latestTime).getTime() - new Date(a.latestTime).getTime())
})

function goHistory(examId: number) {
  router.push(`/student/results/exam/${examId}`)
}

onMounted(async () => {
  try {
    await Promise.all([store.loadMyResults(), store.loadStudentExams()])
  } catch {
    ElMessage.error('加载成绩数据失败，请刷新重试')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div v-loading="loading" class="results-page">
    <StatCards :items="[
      { title: '考试场次', value: examGroups.length, suffix: '场' },
      { title: '提交次数', value: store.results.length, suffix: '次' },
      { title: '最高得分', value: bestScore, suffix: '分' },
      { title: '平均得分', value: store.averageScore, suffix: '分' },
    ]" />

    <div style="margin-bottom: 16px">
      <el-input v-model="searchKeyword" placeholder="搜索考试名称..." clearable prefix-icon="Search" style="max-width: 320px" />
    </div>

    <div class="results-toolbar" v-if="examGroups.length > 0">
      <span class="muted">共 {{ filteredGroups.length }} 场考试，{{ store.results.length }} 条成绩记录</span>
      <el-button size="small" plain @click="downloadFile('/results/export/my', `我的成绩_${new Date().toLocaleDateString()}.xlsx`)">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14" style="margin-right: 4px"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
        导出成绩
      </el-button>
    </div>

    <el-empty v-if="filteredGroups.length === 0 && !searchKeyword" description="暂无成绩，请先参加考试" />
    <el-empty v-if="filteredGroups.length === 0 && searchKeyword" description="没有匹配的成绩" />

    <el-row :gutter="14" v-if="filteredGroups.length > 0">
      <el-col v-for="group in filteredGroups" :key="group.examId" :xs="24" :md="12" class="result-col">
        <el-card shadow="hover" class="result-card" @click="goHistory(group.examId)">
          <div class="result-card__header">
            <strong class="result-card__name">{{ group.examName }}</strong>
            <el-tag size="small" effect="plain" :type="group.allowRetake ? 'success' : 'info'">
              已考 {{ group.submissionCount }} 次
            </el-tag>
          </div>

          <div class="result-card__scores">
            <div class="result-card__score-block">
              <span class="result-card__score-label">最近得分</span>
              <span class="result-card__score-value">{{ group.latestScore }}</span>
            </div>
            <div class="result-card__divider"></div>
            <div class="result-card__score-block">
              <span class="result-card__score-label">最高得分</span>
              <span class="result-card__score-value best">{{ group.bestScore }}</span>
            </div>
          </div>

          <div class="result-card__footer">
            <span class="result-card__time">最近提交：{{ new Date(group.latestTime).toLocaleString() }}</span>
            <span class="result-card__arrow">查看历次成绩 →</span>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.results-page {
  min-height: 200px;
}

.results-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 24px;
  margin-bottom: 16px;
}

.result-col {
  margin-bottom: 14px;
}

.result-card {
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
}

.result-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg) !important;
}

.result-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.result-card__name {
  font-size: var(--text-card-title);
  font-weight: 600;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-card__scores {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 28px;
  padding: 16px 0;
  background: var(--bg-overlay);
  border-radius: var(--radius);
}

.result-card__score-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.result-card__score-label {
  font-size: var(--text-small);
  color: var(--text-tertiary);
}

.result-card__score-value {
  font-size: 28px;
  font-weight: 800;
  color: var(--accent-blue);
}

.result-card__score-value.best {
  color: var(--accent-green);
}

.result-card__divider {
  width: 1px;
  height: 48px;
  background: var(--border-default);
}

.result-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border-subtle);
}

.result-card__time {
  color: var(--text-tertiary);
  font-size: var(--text-small);
}

.result-card__arrow {
  font-size: var(--text-caption);
  color: var(--accent-blue);
  font-weight: 600;
}
</style>
