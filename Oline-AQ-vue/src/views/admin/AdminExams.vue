<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminLayout from './AdminLayout.vue'
import { useExamStore, type Exam, type ExamHistory, type Question } from '@/stores/exam'

const store = useExamStore()
const previewVisible = ref(false)
const previewExam = ref<Exam | null>(null)
const previewQuestions = ref<Question[]>([])
const historyVisible = ref(false)
const currentHistory = ref<ExamHistory[]>([])
const form = reactive({
  examName: '新建基础测试',
  description: '用于课堂快速测验',
  duration: 30,
  startTime: '',
  endTime: '',
  allowRetake: false,
  questionIds: [] as number[],
})

const selectedQuestions = computed(() => store.questions.filter((question) => form.questionIds.includes(question.questionId)))
const selectedScore = computed(() => selectedQuestions.value.reduce((sum, question) => sum + Number(question.score), 0))
const publishedCount = computed(() => store.exams.filter((exam) => exam.status === 'published').length)
const draftCount = computed(() => store.exams.filter((exam) => exam.status === 'draft').length)
const closedCount = computed(() => store.exams.filter((exam) => exam.status === 'closed').length)

onMounted(async () => {
  await Promise.all([store.loadQuestions(), store.loadExams()])
  form.questionIds = store.questions.map((question) => question.questionId)
})

function formatTime(value?: string | null) {
  return value ? new Date(value).toLocaleString() : '未设置'
}

async function createExam() {
  if (!form.examName || form.questionIds.length === 0) {
    ElMessage.warning('请填写考试名称并至少选择一道题')
    return
  }

  await store.createExam({
    examName: form.examName,
    description: form.description,
    duration: form.duration,
    startTime: form.startTime || null,
    endTime: form.endTime || null,
    allowRetake: form.allowRetake,
    questionIds: form.questionIds,
  })
  ElMessage.success('考试已保存为草稿')
}

async function showPreview(row: Exam) {
  const detail = await store.getExamDetail(row.examId)
  previewExam.value = detail.exam
  previewQuestions.value = detail.questions
  previewVisible.value = true
}

async function showHistory(row: Exam) {
  currentHistory.value = await store.loadExamHistory(row.examId)
  previewExam.value = row
  historyVisible.value = true
}

async function handlePublish(exam: Exam) {
  try {
    await ElMessageBox.confirm(`确认发布「${exam.examName}」吗？发布后学生端即可看到该考试。`, '发布考试', { type: 'warning' })
    await store.publishExam(exam.examId)
    ElMessage.success(`「${exam.examName}」已发布`)
  } catch {
    // user cancelled or error handled by interceptor
  }
}

async function handleClose(exam: Exam) {
  try {
    await ElMessageBox.confirm(`确认关闭「${exam.examName}」吗？关闭后学生端将无法参加。`, '关闭考试', { type: 'warning' })
    await store.closeExam(exam.examId)
    ElMessage.success(`「${exam.examName}」已关闭`)
  } catch {
    // user cancelled or error handled by interceptor
  }
}
</script>

<template>
  <AdminLayout title="考试管理" subtitle="支持设置考试开放时间、重考策略，并查看每场考试的历史记录。">
    <el-row :gutter="14">
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" style="margin-bottom: 14px">
          <el-statistic title="全部考试" :value="store.exams.length">
            <template #suffix><span style="font-size: 14px; color: var(--muted)">场</span></template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" style="margin-bottom: 14px">
          <el-statistic title="已发布" :value="publishedCount">
            <template #suffix><span style="font-size: 14px; color: var(--muted)">场</span></template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" style="margin-bottom: 14px">
          <el-statistic title="草稿" :value="draftCount">
            <template #suffix><span style="font-size: 14px; color: var(--muted)">场</span></template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" style="margin-bottom: 14px">
          <el-statistic title="已关闭" :value="closedCount">
            <template #suffix><span style="font-size: 14px; color: var(--muted)">场</span></template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-bottom: 14px">
      <template #header>
        <div style="display: flex; align-items: center; justify-content: space-between">
          <strong>创建考试</strong>
          <span style="color: var(--muted); font-size: 13px">已选 {{ form.questionIds.length }} 道题，预计 {{ selectedScore }} 分</span>
        </div>
      </template>
      <el-form label-position="top">
        <div class="form-grid">
          <el-form-item label="考试名称"><el-input v-model="form.examName" /></el-form-item>
          <el-form-item label="考试说明"><el-input v-model="form.description" /></el-form-item>
          <el-form-item label="时长（分钟）"><el-input-number v-model="form.duration" :min="5" :max="180" style="width: 100%" /></el-form-item>
          <el-form-item label="开始时间">
            <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
          </el-form-item>
          <el-form-item label="结束时间">
            <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
          </el-form-item>
          <el-form-item label="重考策略">
            <el-switch v-model="form.allowRetake" active-text="允许重考" inactive-text="仅允许一次提交" />
          </el-form-item>
        </div>

        <el-form-item label="选择题目">
          <el-checkbox-group v-model="form.questionIds">
            <el-checkbox v-for="question in store.questions" :key="question.questionId" :value="question.questionId">
              {{ question.questionContent }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <el-descriptions :column="3" border style="margin-bottom: 16px">
          <el-descriptions-item label="题目数量">{{ selectedQuestions.length }} 道</el-descriptions-item>
          <el-descriptions-item label="试卷总分">{{ selectedScore }} 分</el-descriptions-item>
          <el-descriptions-item label="重考策略">{{ form.allowRetake ? '允许重考' : '仅一次提交' }}</el-descriptions-item>
        </el-descriptions>
        <el-button type="primary" @click="createExam">保存考试</el-button>
      </el-form>
    </el-card>

    <el-row :gutter="14" v-if="store.exams.length > 0">
      <el-col v-for="exam in store.exams" :key="exam.examId" :xs="24" :md="12" style="margin-bottom: 14px">
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
            <el-descriptions-item label="开始时间">{{ formatTime(exam.startTime) }}</el-descriptions-item>
            <el-descriptions-item label="结束时间">{{ formatTime(exam.endTime) }}</el-descriptions-item>
            <el-descriptions-item label="考试ID">{{ exam.examId }}</el-descriptions-item>
          </el-descriptions>
          <div style="margin-top: 14px; display: flex; flex-wrap: wrap; gap: 8px">
            <el-button type="success" plain @click="showPreview(exam)">预览试卷</el-button>
            <el-button type="info" plain @click="showHistory(exam)">历史记录</el-button>
            <el-button v-if="exam.status === 'draft'" type="primary" plain @click="handlePublish(exam)">发布</el-button>
            <el-button v-if="exam.status === 'published'" type="warning" plain @click="handleClose(exam)">关闭</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-empty v-if="store.exams.length === 0" description="暂无考试，请先创建考试" />

    <el-drawer v-model="previewVisible" title="试卷预览" size="42%">
      <template v-if="previewExam">
        <h3>{{ previewExam.examName }}</h3>
        <p class="muted">{{ previewExam.description }}</p>
        <el-divider />
        <div class="question-list">
          <el-card v-for="(question, index) in previewQuestions" :key="question.questionId" shadow="hover" style="margin-bottom: 14px">
            <strong>{{ index + 1 }}. {{ question.questionContent }}</strong>
            <div class="option-grid">
              <span>A. {{ question.optionA }}</span>
              <span>B. {{ question.optionB }}</span>
              <span v-if="question.optionC">C. {{ question.optionC }}</span>
              <span v-if="question.optionD">D. {{ question.optionD }}</span>
            </div>
            <p class="muted">答案：{{ question.correctAnswer }}，分值：{{ question.score }}</p>
          </el-card>
        </div>
      </template>
    </el-drawer>

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
  </AdminLayout>
</template>
