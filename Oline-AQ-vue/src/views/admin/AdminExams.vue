<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminLayout from './AdminLayout.vue'
import StatCards from '@/views/components/StatCards.vue'
import { useExamStore, type Exam, type ExamHistory, type Question, type QuestionScoreItem } from '@/stores/exam'

const store = useExamStore()
const previewVisible = ref(false)
const previewExam = ref<Exam | null>(null)
const previewQuestions = ref<Question[]>([])
const historyVisible = ref(false)
const currentHistory = ref<ExamHistory[]>([])
const questionPickerVisible = ref(false)
const publishDialogVisible = ref(false)
const publishExamTarget = ref<Exam | null>(null)
const publishMode = ref<'all' | 'select'>('all')
const publishStudentIds = ref<number[]>([])
const creating = ref(false)
const publishing = ref(false)
const form = reactive({
  examName: '新建基础测试',
  description: '用于课堂快速测验',
  duration: 30,
  startTime: '',
  endTime: '',
  allowRetake: false,
  questionIds: [] as number[],
})

const QUESTION_TYPE_LABEL: Record<string, string> = {
  single: '单选',
  judge: '判断',
  short_answer: '简答',
  fill_blank: '填空',
}

const pickerCategory = ref('')
const pickerSearch = ref('')
const selectedSearch = ref('')

const customScores = reactive<Record<number, number>>({})
const selectedQuestions = computed(() => store.questions.filter((q) => form.questionIds.includes(q.questionId)))
const filteredSelected = computed(() => {
  if (!selectedSearch.value) return selectedQuestions.value
  return selectedQuestions.value.filter((q) => q.questionContent.includes(selectedSearch.value))
})
const selectedScore = computed(() => {
  return selectedQuestions.value.reduce((sum, q) => sum + (customScores[q.questionId] ?? q.score), 0)
})

function getScore(question: Question) {
  return customScores[question.questionId] ?? question.score
}

function setScore(questionId: number, value: number) {
  if (value === store.questions.find((q) => q.questionId === questionId)?.score) {
    delete customScores[questionId]
  } else {
    customScores[questionId] = value
  }
}
const publishedCount = computed(() => store.exams.filter((e) => e.status === 'published').length)
const draftCount = computed(() => store.exams.filter((e) => e.status === 'draft').length)
const closedCount = computed(() => store.exams.filter((e) => e.status === 'closed').length)
const categoryOptions = computed(() => {
  const cats = new Set(store.questions.map((q) => q.category).filter(Boolean))
  return Array.from(cats) as string[]
})
const pickerQuestions = computed(() => {
  let list = store.questions
  if (pickerCategory.value) list = list.filter((q) => q.category === pickerCategory.value)
  if (pickerSearch.value) list = list.filter((q) => q.questionContent.includes(pickerSearch.value))
  return list
})
const studentOptions = computed(() => store.users.filter((u) => u.role === 'student'))

onMounted(async () => {
  try {
    await Promise.all([store.loadQuestions(), store.loadExams(), store.loadCategories(), store.loadUsers()])
  } catch {
    ElMessage.error('加载数据失败，请刷新重试')
  }
  form.questionIds = []
})

function formatTime(value?: string | null) {
  return value ? new Date(value).toLocaleString() : '未设置'
}

function openQuestionPicker() {
  pickerCategory.value = ''
  pickerSearch.value = ''
  questionPickerVisible.value = true
}

function clearCustomScores() {
  Object.keys(customScores).forEach((key) => delete customScores[Number(key)])
}

function confirmQuestionSelection() {
  questionPickerVisible.value = false
}

async function createExam() {
  if (!form.examName || form.questionIds.length === 0) {
    ElMessage.warning('请填写考试名称并至少选择一道题')
    return
  }
  creating.value = true
  try {
    const customScoreKeys = Object.keys(customScores)
    const questionScores = customScoreKeys.length > 0
      ? (customScoreKeys.map((id) => ({ questionId: Number(id), score: customScores[Number(id)] })) as QuestionScoreItem[])
      : undefined
    await store.createExam({
      examName: form.examName,
      description: form.description,
      duration: form.duration,
      startTime: form.startTime || null,
      endTime: form.endTime || null,
      allowRetake: form.allowRetake,
      questionIds: form.questionIds,
      questionScores,
    })
    ElMessage.success('考试已保存为草稿')
  } finally {
    creating.value = false
  }
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

function openPublishDialog(exam: Exam) {
  publishExamTarget.value = exam
  publishMode.value = 'all'
  publishStudentIds.value = []
  publishDialogVisible.value = true
}

async function confirmPublish() {
  if (!publishExamTarget.value) return
  publishing.value = true
  try {
    await store.publishExam(publishExamTarget.value.examId, {
      assignAll: publishMode.value === 'all',
      studentIds: publishMode.value === 'select' ? publishStudentIds.value : undefined,
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
</script>

<template>
  <AdminLayout title="考试管理" subtitle="支持设置考试开放时间、重考策略，并查看每场考试的历史记录。">
    <StatCards :items="[
      { title: '全部考试', value: store.exams.length, suffix: '场' },
      { title: '已发布', value: publishedCount, suffix: '场' },
      { title: '草稿', value: draftCount, suffix: '场' },
      { title: '已关闭', value: closedCount, suffix: '场' },
    ]" />

    <!-- 创建考试 -->
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
          <div style="display: flex; align-items: center; gap: 12px">
            <el-button type="primary" @click="openQuestionPicker">打开选题面板</el-button>
            <span class="muted" style="font-size: 13px">已选 {{ form.questionIds.length }} 道题</span>
          </div>
        </el-form-item>

        <el-descriptions :column="3" border style="margin-bottom: 16px">
          <el-descriptions-item label="题目数量">{{ selectedQuestions.length }} 道</el-descriptions-item>
          <el-descriptions-item label="试卷总分">{{ selectedScore }} 分</el-descriptions-item>
          <el-descriptions-item label="重考策略">{{ form.allowRetake ? '允许重考' : '仅一次提交' }}</el-descriptions-item>
        </el-descriptions>
        <el-button type="primary" :loading="creating" @click="createExam">保存考试</el-button>

        <!-- 已选题目列表 -->
        <el-collapse style="margin-top: 16px" v-if="selectedQuestions.length > 0">
          <el-collapse-item title="已选题目列表">
            <el-input v-model="selectedSearch" placeholder="在已选题目中搜索..." clearable style="max-width: 360px; margin-bottom: 12px" />
            <div class="selected-question-list" style="max-height: 400px; overflow-y: auto">
              <div v-for="(question, index) in filteredSelected" :key="question.questionId" class="picker-item">
                <div class="picker-number">{{ index + 1 }}</div>
                <div style="flex: 1; min-width: 0">
                  <div class="picker-content">{{ question.questionContent }}</div>
                  <div class="picker-meta">
                    <el-tag size="small" :type="question.questionType === 'single' ? 'primary' : question.questionType === 'judge' ? 'warning' : 'success'">
                      {{ QUESTION_TYPE_LABEL[question.questionType] || question.questionType }}
                    </el-tag>
                    <el-tag v-if="question.category" size="small" effect="plain">{{ question.category }}</el-tag>
                  </div>
                </div>
                <div style="display: flex; align-items: center; gap: 6px">
                  <el-input-number
                    :model-value="getScore(question)"
                    :min="0"
                    :max="100"
                    :step="1"
                    size="small"
                    style="width: 90px"
                    @update:model-value="(val: number) => setScore(question.questionId, val)"
                  />
                  <span style="font-size: 12px; color: var(--muted)">分</span>
                  <el-button size="small" type="danger" link @click="form.questionIds = form.questionIds.filter((id) => id !== question.questionId)">移除</el-button>
                </div>
              </div>
            </div>
          </el-collapse-item>
        </el-collapse>
      </el-form>
    </el-card>

    <!-- 考试列表 -->
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
            <el-button v-if="exam.status === 'draft'" type="primary" plain @click="openPublishDialog(exam)">发布</el-button>
            <el-button v-if="exam.status === 'published'" type="warning" plain @click="handleClose(exam)">关闭</el-button>
            <el-button type="danger" plain @click="handleDelete(exam)">删除</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-empty v-if="store.exams.length === 0" description="暂无考试，请先创建考试" />

    <!-- 独立选题弹窗 -->
    <el-dialog v-model="questionPickerVisible" title="选择题目" width="800px" top="5vh">
      <div style="display: flex; gap: 12px; margin-bottom: 14px">
        <el-select v-model="pickerCategory" placeholder="按分类筛选" clearable style="width: 180px">
          <el-option v-for="item in categoryOptions" :key="item" :label="item" :value="item" />
        </el-select>
        <el-input v-model="pickerSearch" placeholder="搜索题目内容" clearable style="width: 260px" />
        <span class="muted" style="font-size: 13px; line-height: 32px">显示 {{ pickerQuestions.length }} / {{ store.questions.length }} 道题</span>
      </div>
      <div style="max-height: 60vh; overflow-y: auto">
        <div v-for="(question, index) in pickerQuestions" :key="question.questionId" class="picker-item" :class="{ 'picker-item--selected': form.questionIds.includes(question.questionId) }" @click="
          form.questionIds.includes(question.questionId)
            ? form.questionIds = form.questionIds.filter((id) => id !== question.questionId)
            : form.questionIds.push(question.questionId)
        ">
          <div class="picker-number">{{ index + 1 }}</div>
          <div style="flex: 1; min-width: 0">
            <div class="picker-content">{{ question.questionContent }}</div>
            <div class="picker-meta">
              <el-tag size="small" :type="question.questionType === 'single' ? 'primary' : question.questionType === 'judge' ? 'warning' : 'success'">{{ QUESTION_TYPE_LABEL[question.questionType] || question.questionType }}</el-tag>
              <el-tag v-if="question.category" size="small" effect="plain">{{ question.category }}</el-tag>
              <span>{{ question.score }} 分</span>
            </div>
          </div>
          <el-checkbox :model-value="form.questionIds.includes(question.questionId)" @click.stop />
        </div>
      </div>
      <template #footer>
        <span style="color: var(--muted); font-size: 13px">已选 {{ form.questionIds.length }} 道题</span>
        <el-button type="primary" @click="confirmQuestionSelection">确认选择</el-button>
      </template>
    </el-dialog>

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
      </template>
      <template #footer>
        <el-button @click="publishDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="publishing" :disabled="publishMode === 'select' && publishStudentIds.length === 0" @click="confirmPublish">确认发布</el-button>
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
            <template v-if="question.questionType === 'single' || question.questionType === 'judge'">
              <div class="option-grid">
                <span>A. {{ question.optionA }}</span>
                <span>B. {{ question.optionB }}</span>
                <span v-if="question.optionC">C. {{ question.optionC }}</span>
                <span v-if="question.optionD">D. {{ question.optionD }}</span>
              </div>
            </template>
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
  </AdminLayout>
</template>

<style scoped>
.picker-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid var(--el-border-color);
  border-radius: var(--el-border-radius-base);
  margin-bottom: 8px;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
}
.picker-item:hover {
  background: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary);
}
.picker-item--selected {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}
.picker-number {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--el-color-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
}
.picker-content {
  font-size: 14px;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.picker-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
