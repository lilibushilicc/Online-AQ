<script setup lang="ts">
import { computed, onMounted } from 'vue'
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

onMounted(async () => {
  await Promise.all([store.loadQuestions(), store.loadExams()])
})
</script>

<template>
  <AdminLayout title="教学控制台" subtitle="围绕题库沉淀、考试发布和成绩反馈组织在线测验流程。">
    <el-row :gutter="14">
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" style="margin-bottom: 14px">
          <el-statistic title="题库题目" :value="store.questions.length">
            <template #suffix><span style="font-size: 14px; color: var(--muted)">道</span></template>
          </el-statistic>
          <p style="margin: 8px 0 0; color: var(--muted); font-size: 12px">可用于组卷的有效题量</p>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" style="margin-bottom: 14px">
          <el-statistic title="考试数量" :value="store.exams.length">
            <template #suffix><span style="font-size: 14px; color: var(--muted)">场</span></template>
          </el-statistic>
          <p style="margin: 8px 0 0; color: var(--muted); font-size: 12px">草稿、已发布、已关闭合计</p>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" style="margin-bottom: 14px">
          <el-statistic title="发布率" :value="publishedRate">
            <template #suffix><span style="font-size: 14px; color: var(--muted)">%</span></template>
          </el-statistic>
          <p style="margin: 8px 0 0; color: var(--muted); font-size: 12px">{{ store.publishedExams.length }} 场正在开放</p>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" style="margin-bottom: 14px">
          <el-statistic title="平均得分" :value="store.averageScore">
            <template #suffix><span style="font-size: 14px; color: var(--muted)">分</span></template>
          </el-statistic>
          <p style="margin: 8px 0 0; color: var(--muted); font-size: 12px">来自当前成绩记录</p>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-bottom: 14px">
      <template #header>
        <div style="display: flex; align-items: center; justify-content: space-between">
          <span><strong>测验资产总览</strong> — 累计设计分值 {{ totalScore }}</span>
          <span style="color: var(--muted); font-size: 13px">分值越高，表示题库转化为正式测验的程度越高</span>
        </div>
      </template>
      <div style="display: grid; gap: 16px">
        <div style="display: grid; grid-template-columns: 80px 1fr 60px; gap: 12px; align-items: center">
          <span style="color: var(--muted); font-size: 13px">已发布</span>
          <el-progress :percentage="publishedRate" :stroke-width="10" :show-text="false" />
          <strong>{{ publishedRate }}%</strong>
        </div>
        <div style="display: grid; grid-template-columns: 80px 1fr 60px; gap: 12px; align-items: center">
          <span style="color: var(--muted); font-size: 13px">草稿</span>
          <el-progress :percentage="draftRate" :stroke-width="10" :show-text="false" color="#b56b12" />
          <strong>{{ draftCount }}</strong>
        </div>
        <div style="display: grid; grid-template-columns: 80px 1fr 60px; gap: 12px; align-items: center">
          <span style="color: var(--muted); font-size: 13px">已关闭</span>
          <el-progress :percentage="closedRate" :stroke-width="10" :show-text="false" color="#909399" />
          <strong>{{ closedCount }}</strong>
        </div>
      </div>
    </el-card>

    <el-card style="margin-bottom: 14px">
      <template #header>
        <div style="display: flex; align-items: center; justify-content: space-between">
          <strong>快捷操作</strong>
          <span style="color: var(--muted); font-size: 13px">初版核心流程入口</span>
        </div>
      </template>
      <el-row :gutter="12">
        <el-col :xs="24" :sm="8"><el-button type="primary" plain style="width: 100%" @click="$router.push('/admin/upload')">上传试题</el-button></el-col>
        <el-col :xs="24" :sm="8"><el-button type="success" plain style="width: 100%" @click="$router.push('/admin/exams')">创建考试</el-button></el-col>
        <el-col :xs="24" :sm="8"><el-button type="warning" plain style="width: 100%" @click="$router.push('/admin/results')">查看成绩</el-button></el-col>
      </el-row>
    </el-card>

    <el-card style="margin-bottom: 14px">
      <template #header>
        <strong>初版工作流 — 从导入到评分的完整闭环</strong>
      </template>
      <el-steps :active="5" align-center>
        <el-step title="上传" description="粘贴 TXT 试题文本" />
        <el-step title="解析" description="识别题干、选项、答案" />
        <el-step title="组卷" description="选择题目生成考试" />
        <el-step title="发布" description="学生端出现考试入口" />
        <el-step title="评分" description="提交后自动生成成绩" />
      </el-steps>
    </el-card>

    <el-card>
      <template #header>
        <div style="display: flex; align-items: center; justify-content: space-between">
          <strong>最近考试</strong>
          <el-button link type="primary" @click="$router.push('/admin/exams')">管理考试</el-button>
        </div>
      </template>
      <el-table :data="latestExams" stripe>
        <el-table-column prop="examName" label="考试名称" min-width="180" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 'published' ? 'success' : row.status === 'closed' ? 'info' : 'warning'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="duration" label="时长" width="90" />
        <el-table-column prop="totalScore" label="总分" width="90" />
      </el-table>
    </el-card>
  </AdminLayout>
</template>
