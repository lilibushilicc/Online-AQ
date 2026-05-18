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

onMounted(async () => {
  await Promise.all([store.loadQuestions(), store.loadExams()])
})
</script>

<template>
  <AdminLayout title="教学控制台" subtitle="围绕题库沉淀、考试发布和成绩反馈组织在线测验流程。">
    <section class="metrics">
      <div class="metric"><span>题库题目</span><strong>{{ store.questions.length }}</strong><small>可用于组卷的有效题量</small></div>
      <div class="metric"><span>考试数量</span><strong>{{ store.exams.length }}</strong><small>草稿、已发布、已关闭合计</small></div>
      <div class="metric"><span>发布率</span><strong>{{ publishedRate }}%</strong><small>{{ store.publishedExams.length }} 场正在开放</small></div>
      <div class="metric"><span>平均得分</span><strong>{{ store.averageScore }}</strong><small>来自当前成绩记录</small></div>
    </section>

    <section class="data-strip">
      <div class="data-strip-main">
        <span class="muted">测验资产总览</span>
        <strong>{{ totalScore }}</strong>
        <p class="muted">当前所有考试累计设计分值。分值越高，表示题库转化为正式测验的程度越高。</p>
      </div>
      <div class="bars">
        <div class="bar-row">
          <span>已发布</span>
          <div class="bar-track"><span class="bar-fill" :style="{ width: `${publishedRate}%` }"></span></div>
          <b>{{ publishedRate }}%</b>
        </div>
        <div class="bar-row">
          <span>草稿</span>
          <div class="bar-track"><span class="bar-fill" :style="{ width: `${store.exams.length ? Math.round((draftCount / store.exams.length) * 100) : 0}%` }"></span></div>
          <b>{{ draftCount }}</b>
        </div>
        <div class="bar-row">
          <span>已关闭</span>
          <div class="bar-track"><span class="bar-fill" :style="{ width: `${store.exams.length ? Math.round((closedCount / store.exams.length) * 100) : 0}%` }"></span></div>
          <b>{{ closedCount }}</b>
        </div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">
        <h3>快捷操作</h3>
        <span class="muted">初版核心流程入口</span>
      </div>
      <el-row :gutter="12">
        <el-col :xs="24" :sm="8"><el-button type="primary" plain style="width: 100%" @click="$router.push('/admin/upload')">上传试题</el-button></el-col>
        <el-col :xs="24" :sm="8"><el-button type="success" plain style="width: 100%" @click="$router.push('/admin/exams')">创建考试</el-button></el-col>
        <el-col :xs="24" :sm="8"><el-button type="warning" plain style="width: 100%" @click="$router.push('/admin/results')">查看成绩</el-button></el-col>
      </el-row>
    </section>

    <section class="panel">
      <div class="panel-title">
        <h3>初版工作流</h3>
        <span class="muted">从导入到评分的完整闭环</span>
      </div>
      <div class="workflow">
        <div class="workflow-step"><b>1 上传</b><span class="muted">粘贴 TXT 试题文本</span></div>
        <div class="workflow-step"><b>2 解析</b><span class="muted">识别题干、选项、答案</span></div>
        <div class="workflow-step"><b>3 组卷</b><span class="muted">选择题目生成考试</span></div>
        <div class="workflow-step"><b>4 发布</b><span class="muted">学生端出现考试入口</span></div>
        <div class="workflow-step"><b>5 评分</b><span class="muted">提交后自动生成成绩</span></div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">
        <h3>最近考试</h3>
        <el-button link type="primary" @click="$router.push('/admin/exams')">管理考试</el-button>
      </div>
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
    </section>
  </AdminLayout>
</template>
