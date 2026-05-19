<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import AdminLayout from './AdminLayout.vue'
import { useExamStore } from '@/stores/exam'

const store = useExamStore()
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
  { label: '题库题目', value: store.questions.length, unit: '道', desc: '可用于组卷的有效题量' },
  { label: '考试数量', value: store.exams.length, unit: '场', desc: '草稿、已发布、已关闭合计' },
  { label: '发布率', value: publishedRate.value, unit: '%', desc: `${store.publishedExams.length} 场正在开放` },
  { label: '平均得分', value: averageScore.value, unit: '分', desc: '来自当前成绩记录' },
])

onMounted(async () => {
  try {
    await Promise.all([store.loadQuestions(), store.loadExams()])
  } catch {
    ElMessage.error('加载数据失败，请刷新重试')
  }
})
</script>

<template>
  <AdminLayout title="教学控制台" subtitle="围绕题库沉淀、考试发布和成绩反馈组织在线测验流程。">
    <div class="bento-grid">
      <div
        v-for="(cell, index) in cells"
        :key="cell.label"
        class="bento-cell fade-in"
        :class="[`stagger-${index + 1}`]"
      >
        <div class="bento-label">{{ cell.label }}</div>
        <div class="bento-value">
          {{ cell.value }}
          <span style="font-size: 15px; font-weight: 400; color: var(--muted); letter-spacing: 0;">{{ cell.unit }}</span>
        </div>
        <div class="bento-trend">{{ cell.desc }}</div>
      </div>

      <div class="bento-cell bento-cell-wide fade-in stagger-5">
        <div class="bento-label">测验资产总览</div>
        <div style="display: grid; gap: 12px; margin-top: 4px">
          <div style="display: grid; grid-template-columns: 64px 1fr 40px; gap: 10px; align-items: center">
            <span style="color: var(--muted); font-size: 12px">已发布</span>
            <el-progress :percentage="publishedRate" :stroke-width="8" :show-text="false" color="var(--ink-green)" />
            <strong style="font-size: 13px">{{ publishedRate }}%</strong>
          </div>
          <div style="display: grid; grid-template-columns: 64px 1fr 40px; gap: 10px; align-items: center">
            <span style="color: var(--muted); font-size: 12px">草稿</span>
            <el-progress :percentage="draftRate" :stroke-width="8" :show-text="false" color="var(--amber)" />
            <strong style="font-size: 13px">{{ draftCount }}</strong>
          </div>
          <div style="display: grid; grid-template-columns: 64px 1fr 40px; gap: 10px; align-items: center">
            <span style="color: var(--muted); font-size: 12px">已关闭</span>
            <el-progress :percentage="closedRate" :stroke-width="8" :show-text="false" color="#94a3b8" />
            <strong style="font-size: 13px">{{ closedCount }}</strong>
          </div>
        </div>
        <div class="bento-trend" style="margin-top: 10px">
          累计设计分值 <strong>{{ totalScore }}</strong> 分 | 总计时长 <strong>{{ totalDuration }}</strong> 分钟
        </div>
      </div>

      <div class="bento-cell fade-in stagger-6" style="display: flex; flex-direction: column;">
        <div class="bento-label">快捷操作</div>
        <div style="display: flex; flex-direction: column; gap: 8px; flex: 1; justify-content: center;">
          <el-button type="primary" plain size="small" @click="$router.push('/admin/upload')">
            上传试题
          </el-button>
          <el-button type="success" plain size="small" @click="$router.push('/admin/exams')">
            创建考试
          </el-button>
          <el-button type="warning" plain size="small" @click="$router.push('/admin/results')">
            查看成绩
          </el-button>
        </div>
      </div>

      <div class="bento-cell bento-cell-full fade-in stagger-7" style="padding: 0;">
        <div style="padding: 16px 20px; border-bottom: 1px solid var(--line-light);">
          <span style="font-size: 11px; font-weight: 600; color: var(--muted); text-transform: uppercase; letter-spacing: 0.5px;">工作流程</span>
        </div>
        <div style="padding: 16px 20px">
          <el-steps :active="5" align-center>
            <el-step title="上传" description="TXT / DOCX" />
            <el-step title="解析" description="题干与答案" />
            <el-step title="组卷" description="选择题目" />
            <el-step title="发布" description="学生端可见" />
            <el-step title="评分" description="自动生成成绩" />
          </el-steps>
        </div>
      </div>

      <div class="bento-cell bento-cell-full fade-in stagger-8" style="padding: 0;">
        <div style="padding: 16px 20px; border-bottom: 1px solid var(--line-light); display: flex; align-items: center; justify-content: space-between;">
          <span style="font-size: 11px; font-weight: 600; color: var(--muted); text-transform: uppercase; letter-spacing: 0.5px;">最近考试</span>
          <el-button link type="primary" size="small" @click="$router.push('/admin/exams')">管理考试</el-button>
        </div>
        <div style="padding: 0">
          <el-table :data="latestExams" stripe>
            <el-table-column prop="examName" label="考试名称" min-width="180" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'published' ? 'success' : row.status === 'closed' ? 'info' : 'warning'" size="small">
                  {{ row.status === 'published' ? '已发布' : row.status === 'closed' ? '已关闭' : '草稿' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="duration" label="时长" width="70" />
            <el-table-column prop="totalScore" label="总分" width="70" />
          </el-table>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>