<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useExamStore, type FeedbackListVO } from '@/stores/exam'

const store = useExamStore()

interface FeedbackDetail {
  studentName: string
  feedback: { feedbackType: string; description: string; createTime: string; status: string; rejectReason?: string }
  question: { questionContent: string; questionType: string; optionA: string; optionB: string; optionC?: string; optionD?: string; correctAnswer: string; score: number; category?: string }
}

const activeTab = ref('')
const feedbacks = ref<FeedbackListVO[]>([])
const loading = ref(false)

const detailDrawerVisible = ref(false)
const detailData = ref<FeedbackDetail | null>(null)

const resolveDialogVisible = ref(false)
const resolveTarget = ref<FeedbackListVO | null>(null)
const editForm = ref({
  questionContent: '',
  questionType: 'single',
  optionA: '',
  optionB: '',
  optionC: '',
  optionD: '',
  correctAnswer: '',
  score: 5,
  category: '',
})

const rejectDialogVisible = ref(false)
const rejectTarget = ref<FeedbackListVO | null>(null)
const rejectReason = ref('')

const tabs = [
  { label: '全部', value: '' },
  { label: '待处理', value: 'pending' },
  { label: '已采纳', value: 'resolved' },
  { label: '已驳回', value: 'rejected' },
]

const feedbackTypeMap: Record<string, string> = {
  answer_error: '答案错误',
  content_error: '题干错误',
  option_error: '选项错误',
  other: '其他问题',
}

async function loadData(status?: string) {
  loading.value = true
  try {
    feedbacks.value = await store.loadFeedbacks(status || undefined)
  } finally {
    loading.value = false
  }
}

function onTabChange(value: string) {
  activeTab.value = value
  loadData(value)
}

async function openDetail(item: FeedbackListVO) {
  const data = await store.getFeedbackDetail(item.feedbackId)
  detailData.value = data as FeedbackDetail
  detailDrawerVisible.value = true
}

function openResolve(item: FeedbackListVO) {
  resolveTarget.value = item
  editForm.value = {
    questionContent: '',
    questionType: 'single',
    optionA: '',
    optionB: '',
    optionC: '',
    optionD: '',
    correctAnswer: '',
    score: 5,
    category: '',
  }
  resolveDialogVisible.value = true
}

async function confirmResolve() {
  if (!resolveTarget.value) return
  const otherCount = (resolveTarget.value.pendingCount || 1) - 1
  if (otherCount > 0) {
    try {
      await ElMessageBox.confirm(
        `该题目还有 ${otherCount} 条其他学生的待处理反馈，确认采纳后将一并标记为已采纳`,
        '确认处理',
        { type: 'info', confirmButtonText: '全部采纳', cancelButtonText: '取消' },
      )
    } catch {
      return
    }
  }
  const payload: Record<string, unknown> = {}
  if (editForm.value.questionContent) payload.questionContent = editForm.value.questionContent
  if (editForm.value.questionType) payload.questionType = editForm.value.questionType
  if (editForm.value.optionA) payload.optionA = editForm.value.optionA
  if (editForm.value.optionB) payload.optionB = editForm.value.optionB
  if (editForm.value.optionC) payload.optionC = editForm.value.optionC
  if (editForm.value.optionD) payload.optionD = editForm.value.optionD
  if (editForm.value.correctAnswer) payload.correctAnswer = editForm.value.correctAnswer
  payload.score = editForm.value.score
  if (editForm.value.category) payload.category = editForm.value.category
  try {
    const res = await store.resolveFeedback(resolveTarget.value.feedbackId, payload)
    ElMessage.success('题目已修改，反馈已采纳')
    resolveDialogVisible.value = false
    await loadData(activeTab.value)
  } catch {
    ElMessage.error('操作失败')
  }
}

async function confirmReject() {
  if (!rejectTarget.value) return
  const otherCount = (rejectTarget.value.pendingCount || 1) - 1
  if (otherCount > 0) {
    try {
      await ElMessageBox.confirm(
        `该题目还有 ${otherCount} 条其他学生的待处理反馈，确认驳回后将一并驳回`,
        '确认处理',
        { type: 'warning', confirmButtonText: '全部驳回', cancelButtonText: '取消' },
      )
    } catch {
      return
    }
  }
  try {
    const res = await store.rejectFeedback(rejectTarget.value.feedbackId, rejectReason.value || '无')
    ElMessage.success('反馈已驳回')
    rejectDialogVisible.value = false
    await loadData(activeTab.value)
  } catch {
    ElMessage.error('操作失败')
  }
}

function openReject(item: FeedbackListVO) {
  rejectTarget.value = item
  rejectReason.value = ''
  rejectDialogVisible.value = true
}

function getStatusTag(status?: string) {
  if (status === 'pending') return { type: 'warning' as const, text: '待处理' }
  if (status === 'resolved') return { type: 'success' as const, text: '已采纳' }
  if (status === 'rejected') return { type: 'info' as const, text: '已驳回' }
  return { type: 'info' as const, text: status ?? '未知' }
}

onMounted(() => loadData())
</script>

<template>
    <el-card>
      <el-tabs :model-value="activeTab" @tab-change="onTabChange">
        <el-tab-pane v-for="tab in tabs" :key="tab.value" :label="tab.label" :name="tab.value" />
      </el-tabs>

        <el-table v-loading="loading" :data="feedbacks" stripe style="margin-top: 12px">
        <el-table-column prop="feedbackId" label="ID" width="55" />
        <el-table-column prop="questionContent" label="题目内容" min-width="240" show-overflow-tooltip />
        <el-table-column label="同题反馈" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.pendingCount > 1 && row.status === 'pending'" type="warning" size="small">
              {{ row.pendingCount }} 人
            </el-tag>
            <span v-else class="muted" style="font-size: 12px">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="studentName" label="反馈学生" width="110" />
        <el-table-column label="反馈类型" width="90">
          <template #default="{ row }">
            {{ feedbackTypeMap[row.feedbackType] || row.feedbackType }}
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="85">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.status).type" size="small">{{ getStatusTag(row.status).text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="160">
          <template #default="{ row }">
            {{ new Date(row.createTime).toLocaleString() }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="195" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">查看</el-button>
            <el-button
              v-if="row.status === 'pending'"
              link
              type="success"
              size="small"
              @click="openResolve(row)"
            >修改并采纳</el-button>
            <el-button
              v-if="row.status === 'pending'"
              link
              type="danger"
              size="small"
              @click="openReject(row)"
            >驳回</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && feedbacks.length === 0" description="暂无反馈" />
    </el-card>

    <!-- 详情 Drawer -->
    <el-drawer v-model="detailDrawerVisible" title="反馈详情" size="48%">
      <template v-if="detailData">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="反馈学生">{{ detailData.studentName }}</el-descriptions-item>
          <el-descriptions-item label="反馈类型">{{ feedbackTypeMap[detailData.feedback.feedbackType] || detailData.feedback.feedbackType }}</el-descriptions-item>
          <el-descriptions-item label="反馈描述">{{ detailData.feedback.description }}</el-descriptions-item>
          <el-descriptions-item label="反馈时间">{{ new Date(detailData.feedback.createTime).toLocaleString() }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusTag(detailData.feedback.status).type" size="small">{{ getStatusTag(detailData.feedback.status).text }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="detailData.feedback.rejectReason" label="驳回理由">{{ detailData.feedback.rejectReason }}</el-descriptions-item>
        </el-descriptions>

        <el-divider />
        <h3 style="margin: 0 0 12px">当前题目信息</h3>
        <el-card shadow="never">
          <p><strong>{{ detailData.question.questionContent }}</strong></p>
          <div v-if="detailData.question.questionType === 'single' || detailData.question.questionType === 'judge'" style="margin: 8px 0">
            <div>A. {{ detailData.question.optionA }}</div>
            <div>B. {{ detailData.question.optionB }}</div>
            <div v-if="detailData.question.optionC">C. {{ detailData.question.optionC }}</div>
            <div v-if="detailData.question.optionD">D. {{ detailData.question.optionD }}</div>
          </div>
          <div style="margin-top: 8px">
            <el-tag>答案：{{ detailData.question.correctAnswer }}</el-tag>
            <el-tag>{{ detailData.question.score }} 分</el-tag>
            <el-tag v-if="detailData.question.category" effect="plain">{{ detailData.question.category }}</el-tag>
          </div>
        </el-card>
      </template>
    </el-drawer>

    <!-- 修改并采纳弹窗 -->
    <el-dialog v-model="resolveDialogVisible" title="修改题目并采纳反馈" width="600px">
      <template v-if="resolveTarget">
        <p style="color: var(--muted); margin-bottom: 16px">反馈题目：<strong>{{ resolveTarget.questionContent }}</strong></p>
        <el-form label-position="top">
          <el-form-item label="题干">
            <el-input v-model="editForm.questionContent" placeholder="留空表示不修改" />
          </el-form-item>
          <el-row :gutter="10">
            <el-col :span="12">
              <el-form-item label="选项 A"><el-input v-model="editForm.optionA" placeholder="留空不修改" /></el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="选项 B"><el-input v-model="editForm.optionB" placeholder="留空不修改" /></el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="选项 C"><el-input v-model="editForm.optionC" placeholder="留空不修改" /></el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="选项 D"><el-input v-model="editForm.optionD" placeholder="留空不修改" /></el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="10">
            <el-col :span="8">
              <el-form-item label="正确答案">
                <el-input v-model="editForm.correctAnswer" placeholder="如: A" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="分值">
                <el-input-number v-model="editForm.score" :min="1" :max="100" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="分类">
                <el-input v-model="editForm.category" placeholder="留空不修改" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="resolveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmResolve">保存并采纳</el-button>
      </template>
    </el-dialog>

    <!-- 驳回弹窗 -->
    <el-dialog v-model="rejectDialogVisible" title="驳回反馈" width="450px">
      <template v-if="rejectTarget">
        <p style="margin-bottom: 12px">反馈题目：<strong>{{ rejectTarget.questionContent }}</strong></p>
        <el-form label-position="top">
          <el-form-item label="驳回理由">
            <el-input v-model="rejectReason" type="textarea" :rows="3" placeholder="请说明驳回原因..." />
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmReject">确认驳回</el-button>
      </template>
    </el-dialog>
</template>