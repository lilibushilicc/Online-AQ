<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import StatCards from '@/views/components/StatCards.vue'
import QuestionOptions from '@/views/components/QuestionOptions.vue'
import { useExamStore, type Exam, type ExamHistory, type Question } from '@/stores/exam'
import * as api from '@/api'
import { formatTime } from '@/utils/format'

const router = useRouter()
const store = useExamStore()

const publishDialogVisible = ref(false)
const publishExamTarget = ref<Exam | null>(null)
const publishShuffleQuestions = ref(false)
const publishShuffleAnswers = ref(false)
const publishMode = ref('all')
const publishStudentIds = ref<number[]>([])
const publishing = ref(false)
const previewVisible = ref(false)
const previewExam = ref<Exam | null>(null)
const previewQuestions = ref<Question[]>([])
const historyVisible = ref(false)
const currentHistory = ref<ExamHistory[]>([])
const examSearch = ref('')
const settingsDialogVisible = ref(false)
const settingsExam = ref<Exam | null>(null)
const settingsEndTime = ref<string | null>(null)
const settingsAllowRetake = ref(false)
const settingsSaving = ref(false)

const filteredExams = computed(() => {
  if (!examSearch.value) return store.exams
  return store.exams.filter((e) => e.examName.toLowerCase().includes(examSearch.value.toLowerCase()))
})
const publishedCount = computed(() => store.exams.filter((e) => e.status === 'published').length)
const draftCount = computed(() => store.exams.filter((e) => e.status === 'draft').length)
const closedCount = computed(() => store.exams.filter((e) => e.status === 'closed').length)
const studentOptions = computed(() => store.users.filter((u) => u.role === 'student'))

onMounted(async () => {
  try {
    await Promise.all([store.loadExams(), store.loadUsers()])
  } catch {
    ElMessage.error('加载数据失败，请刷新重试')
  }
})

async function showPreview(row: Exam) {
  const detail = await api.getExamDetailApi(row.examId)
  previewExam.value = detail.exam
  previewQuestions.value = detail.questions
  previewVisible.value = true
}

async function showHistory(row: Exam) {
  currentHistory.value = await api.loadExamHistoryApi(row.examId)
  previewExam.value = row
  historyVisible.value = true
}

function openPublishDialog(exam: Exam) {
  publishExamTarget.value = exam
  publishMode.value = 'all'
  publishStudentIds.value = []
  publishShuffleQuestions.value = false
  publishShuffleAnswers.value = false
  publishDialogVisible.value = true
}

async function confirmPublish() {
  if (!publishExamTarget.value) return
  publishing.value = true
  try {
    await store.publishExam(publishExamTarget.value.examId, {
      assignAll: publishMode.value === 'all',
      studentIds: publishMode.value === 'select' ? publishStudentIds.value : undefined,
      shuffleQuestions: publishShuffleQuestions.value,
      shuffleAnswers: publishShuffleAnswers.value,
    })
    ElMessage.success(`「${publishExamTarget.value.examName}」已发布`)
    publishDialogVisible.value = false
  } finally {
    publishing.value = false
  }
}

async function handleClose(exam: Exam) {
  try {
    await ElMessageBox.confirm(`确认关闭「${exam.examName}」吗？关闭后学生端将无法参加。`, '关闭考试', { type: 'warning' })
    await store.closeExam(exam.examId)
    ElMessage.success(`「${exam.examName}」已关闭`)
  } catch {
    // user cancelled
  }
}

async function handleDelete(exam: Exam) {
  try {
    await ElMessageBox.confirm(`确认永久删除「${exam.examName}」吗？相关数据（成绩、答案等）也将一并删除，不可恢复。`, '删除考试', { type: 'error', confirmButtonText: '确认删除', confirmButtonClass: 'el-button--danger' })
    await store.deleteExam(exam.examId)
    ElMessage.success(`「${exam.examName}」已删除`)
  } catch {
    // user cancelled
  }
}

function openSettingsDialog(exam: Exam) {
  settingsExam.value = exam
  settingsEndTime.value = exam.endTime ?? null
  settingsAllowRetake.value = exam.allowRetake ?? false
  settingsDialogVisible.value = true
}

async function confirmSettings() {
  if (!settingsExam.value) return
  settingsSaving.value = true
  try {
    const payload: { endTime?: string | null; allowRetake?: boolean } = {}
    if (settingsEndTime.value !== settingsExam.value.endTime) {
      payload.endTime = settingsEndTime.value
    }
    if (settingsAllowRetake.value !== settingsExam.value.allowRetake) {
      payload.allowRetake = settingsAllowRetake.value
    }
    await api.updateExamSettingsApi(settingsExam.value.examId, payload)
    ElMessage.success(`「${settingsExam.value.examName}」设置已更新`)
    settingsDialogVisible.value = false
    await store.loadExams()
  } finally {
    settingsSaving.value = false
  }
}
</script>

<template>
    <StatCards :items="[
      { title: '全部考试', value: store.exams.length, suffix: '场' },
      { title: '已发布', value: publishedCount, suffix: '场' },
      { title: '草稿', value: draftCount, suffix: '场' },
      { title: '已关闭', value: closedCount, suffix: '场' },
    ]" />

    <!-- 考试列表 -->
    <el-card>
      <template #header>
        <div style="display: flex; align-items: center; justify-content: space-between">
          <strong>考试列表（{{ filteredExams.length }}）</strong>
          <el-input v-model="examSearch" placeholder="搜索考试名称..." clearable style="max-width: 260px" />
        </div>
      </template>
      <el-row :gutter="14" v-if="filteredExams.length > 0">
        <el-col v-for="exam in filteredExams" :key="exam.examId" :xs="24" :md="12" style="margin-bottom: 14px">
          <el-card shadow="hover">
            <template #header>
              <div style="display: flex; align-items: center; justify-content: space-between">
                <strong>{{ exam.examName }}</strong>
                <el-tag :type="exam.status === 'published' ? 'success' : exam.status === 'closed' ? 'info' : 'warning'">
                  {{ exam.status }}
                </el-tag>
              </div>
            </template>
            <p style="color: var(--muted); font-size: 13px; margin: 0 0 12px">{{ exam.description || '暂无考试说明' }}</p>
            <el-descriptions :column="3" border size="small">
              <el-descriptions-item label="时长">{{ exam.duration }} 分钟</el-descriptions-item>
              <el-descriptions-item label="总分">{{ exam.totalScore }} 分</el-descriptions-item>
              <el-descriptions-item label="重考">{{ exam.allowRetake ? '允许' : '禁止' }}</el-descriptions-item>
              <el-descriptions-item label="乱序">{{ [exam.shuffleQuestions && '题目', exam.shuffleAnswers && '选项'].filter(Boolean).join('+') || '无' }}</el-descriptions-item>
              <el-descriptions-item label="开始时间">{{ formatTime(exam.startTime) }}</el-descriptions-item>
              <el-descriptions-item label="结束时间">{{ formatTime(exam.endTime) }}</el-descriptions-item>
              <el-descriptions-item label="考试ID">{{ exam.examId }}</el-descriptions-item>
            </el-descriptions>
            <div class="exam-actions">
              <span class="exam-action-group">
                <el-button size="small" plain @click="showPreview(exam)">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14" style="margin-right: 4px"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                  预览试卷
                </el-button>
                <el-button size="small" plain @click="showHistory(exam)">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14" style="margin-right: 4px"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                  历史记录
                </el-button>
              </span>
              <span class="exam-action-divider" />
              <span class="exam-action-group">
                <el-button v-if="exam.status === 'draft'" size="small" plain @click="router.push({ path: '/admin/papers' })">组卷</el-button>
                <el-button v-if="exam.status === 'draft'" type="primary" size="small" @click="openPublishDialog(exam)">发布</el-button>
                <el-button v-if="exam.status !== 'draft'" size="small" plain @click="openSettingsDialog(exam)">设置</el-button>
                <el-button v-if="exam.status === 'published'" type="warning" plain size="small" @click="handleClose(exam)">关闭</el-button>
                <el-button type="danger" plain size="small" @click="handleDelete(exam)">删除</el-button>
              </span>
            </div>
          </el-card>
        </el-col>
      </el-row>
      <el-empty v-if="filteredExams.length === 0 && !examSearch" description="暂无考试，请先前往试卷管理创建试卷" />
      <el-empty v-if="filteredExams.length === 0 && examSearch" description="没有匹配的考试" />
    </el-card>

    <!-- 发布弹窗 -->
    <el-dialog v-model="publishDialogVisible" title="发布考试" width="500px">
      <template v-if="publishExamTarget">
        <p style="margin-bottom: 16px">考试：<strong>{{ publishExamTarget.examName }}</strong></p>
        <el-radio-group v-model="publishMode" style="margin-bottom: 16px">
          <el-radio value="all">全体学生</el-radio>
          <el-radio value="select">仅指定学生</el-radio>
        </el-radio-group>

        <template v-if="publishMode === 'select'">
          <el-checkbox-group v-model="publishStudentIds">
            <el-checkbox v-for="s in studentOptions" :key="s.userId" :value="s.userId" style="margin-bottom: 6px">
              {{ s.realName }}（{{ s.username }}）
            </el-checkbox>
          </el-checkbox-group>
          <div v-if="publishStudentIds.length === 0" style="margin-top: 8px">
            <span class="muted">请至少选择一名学生</span>
          </div>
        </template>

        <el-divider />
        <p style="font-weight: 600; margin-bottom: 12px">乱序配置（发布后可关闭再发布不同版本）</p>
        <div style="display: flex; flex-direction: column; gap: 10px">
          <el-switch v-model="publishShuffleQuestions" active-text="题目乱序" inactive-text="题目按创建顺序" />
          <el-switch v-model="publishShuffleAnswers" active-text="选项乱序" inactive-text="选项按原始顺序" />
        </div>
      </template>
      <template #footer>
        <el-button @click="publishDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="publishing" :disabled="publishMode === 'select' && publishStudentIds.length === 0" @click="confirmPublish">确认发布</el-button>
      </template>
    </el-dialog>

    <!-- 考试设置弹窗 -->
    <el-dialog v-model="settingsDialogVisible" title="考试设置" width="460px">
      <template v-if="settingsExam">
        <p style="margin-bottom: 16px">考试：<strong>{{ settingsExam.examName }}</strong></p>
        <el-form label-position="top">
          <el-form-item label="截止时间">
            <el-date-picker v-model="settingsEndTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" placeholder="不修改则保持原时间" />
          </el-form-item>
          <el-form-item label="是否允许重做">
            <el-switch v-model="settingsAllowRetake" active-text="允许" inactive-text="仅一次提交" />
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="settingsDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="settingsSaving" @click="confirmSettings">保存设置</el-button>
      </template>
    </el-dialog>

    <!-- 试卷预览 -->
    <el-drawer v-model="previewVisible" title="试卷预览" size="42%">
      <template v-if="previewExam">
        <h3>{{ previewExam.examName }}</h3>
        <p class="muted">{{ previewExam.description }}</p>
        <el-divider />
        <div class="question-list">
          <el-card v-for="(question, index) in previewQuestions" :key="question.questionId" shadow="hover" style="margin-bottom: 14px">
            <strong>{{ index + 1 }}. {{ question.questionContent }}</strong>
            <QuestionOptions :question="question" />
            <p class="muted">答案：{{ question.correctAnswer }}，分值：{{ question.score }}</p>
          </el-card>
        </div>
      </template>
    </el-drawer>

    <!-- 历史记录 -->
    <el-drawer v-model="historyVisible" title="考试历史记录" size="42%">
      <template v-if="previewExam">
        <h3>{{ previewExam.examName }}</h3>
        <p class="muted">查看该考试的创建、发布、关闭和提交历史。</p>
        <el-divider />
        <div class="question-list">
          <el-card v-for="history in currentHistory" :key="history.historyId" shadow="hover" style="margin-bottom: 14px">
            <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px">
              <strong>{{ history.actionType }}</strong>
              <span class="muted">{{ history.operatorName || `用户 #${history.operatorId ?? '-'}` }} · {{ new Date(history.createTime).toLocaleString() }}</span>
            </div>
            <p class="muted">{{ history.actionDetail }}</p>
          </el-card>
          <el-empty v-if="currentHistory.length === 0" description="暂无历史记录" />
        </div>
      </template>
    </el-drawer>
</template>

<style scoped>
.exam-actions {
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.exam-action-group {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}
.exam-action-divider {
  width: 1px;
  height: 24px;
  background: var(--line-light);
  flex-shrink: 0;
}
</style>
