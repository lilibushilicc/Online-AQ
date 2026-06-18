<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import QuestionOptions from '@/views/components/QuestionOptions.vue'
import { useExamStore, type Exam, type ExamHistory, type Question, type QuestionScoreItem, type UploadFileItem } from '@/stores/exam'
import { QUESTION_TYPE_LABEL } from '@/constants'
import * as api from '@/api'
import { formatTime } from '@/utils/format'

const router = useRouter()
const store = useExamStore()
const uploadFiles = ref<UploadFileItem[]>([])
const pickerCategory = ref('')
const pickerFileId = ref<number | null>(null)
const pickerSearch = ref('')
const selectedSearch = ref('')

const customScores = reactive<Record<number, number>>({})
const form = reactive({ examName: '', description: '', duration: 60, startTime: null as string | null, endTime: null as string | null, allowRetake: false, questionIds: [] as number[] })
const editingPaperId = ref<number | null>(null)
const questionPickerVisible = ref(false)
const saving = ref(false)
const drafts = ref<Exam[]>([])
const draftSearch = ref('')

const filteredDrafts = computed(() => {
  if (!draftSearch.value) return drafts.value
  const kw = draftSearch.value.toLowerCase()
  return drafts.value.filter((d) => d.examName.toLowerCase().includes(kw))
})

const selectedQuestions = computed(() => store.questions.filter((q) => form.questionIds.includes(q.questionId)))
const filteredSelected = computed(() => {
  if (!selectedSearch.value) return selectedQuestions.value
  return selectedQuestions.value.filter((q) => q.questionContent.includes(selectedSearch.value))
})
const selectedScore = computed(() => {
  return selectedQuestions.value.reduce((sum, q) => sum + (customScores[q.questionId] ?? q.score), 0)
})
const categoryOptions = computed(() => {
  const cats = new Set(store.questions.map((q) => q.category).filter(Boolean))
  return Array.from(cats) as string[]
})
const pickerQuestions = computed(() => {
  let list = store.questions
  if (pickerCategory.value) list = list.filter((q) => q.category === pickerCategory.value)
  if (pickerFileId.value) list = list.filter((q) => q.sourceFileId === pickerFileId.value)
  if (pickerSearch.value) list = list.filter((q) => q.questionContent.includes(pickerSearch.value))
  return list
})

const pickerAllSelected = computed(() => {
  return pickerQuestions.value.length > 0 && pickerQuestions.value.every((q) => form.questionIds.includes(q.questionId))
})

const pickerIndeterminate = computed(() => {
  if (pickerQuestions.value.length === 0) return false
  const count = pickerQuestions.value.filter((q) => form.questionIds.includes(q.questionId)).length
  return count > 0 && count < pickerQuestions.value.length
})

function togglePickerSelectAll(val: boolean) {
  if (val) {
    const existing = new Set(form.questionIds)
    pickerQuestions.value.forEach((q) => {
      if (!existing.has(q.questionId)) {
        form.questionIds.push(q.questionId)
      }
    })
  } else {
    const ids = new Set(pickerQuestions.value.map((q) => q.questionId))
    form.questionIds = form.questionIds.filter((id) => !ids.has(id))
  }
}

onMounted(async () => {
  try {
    await Promise.all([store.loadQuestions(), store.loadExams(), store.loadCategories()])
    uploadFiles.value = await api.loadUploadFilesApi()
    drafts.value = store.exams.filter((e) => e.status === 'draft')
  } catch {
    ElMessage.error('加载数据失败，请刷新重试')
  }
})

function resetForm() {
  form.examName = ''
  form.description = ''
  form.duration = 60
  form.startTime = null
  form.endTime = null
  form.allowRetake = false
  form.questionIds = []
  Object.keys(customScores).forEach((k) => delete customScores[Number(k)])
  editingPaperId.value = null
}

function getScore(question: Question) {
  return customScores[question.questionId] ?? question.score
}

function setScore(questionId: number, value: number) {
  const orig = store.questions.find((q) => q.questionId === questionId)?.score
  if (value === orig) {
    delete customScores[questionId]
  } else {
    customScores[questionId] = value
  }
}

function openQuestionPicker() {
  pickerCategory.value = ''
  pickerFileId.value = null
  pickerSearch.value = ''
  questionPickerVisible.value = true
}

function confirmQuestionSelection() {
  questionPickerVisible.value = false
}

async function savePaper() {
  if (!form.examName || form.questionIds.length === 0) {
    ElMessage.warning('请填写试卷名称并至少选择一道题')
    return
  }
  saving.value = true
  try {
    const customScoreKeys = Object.keys(customScores)
    const questionScores = customScoreKeys.length > 0
      ? (customScoreKeys.map((id) => ({ questionId: Number(id), score: customScores[Number(id)] })) as QuestionScoreItem[])
      : undefined
    const payload = {
      examName: form.examName,
      description: form.description,
      duration: form.duration,
      startTime: form.startTime || null,
      endTime: form.endTime || null,
      allowRetake: form.allowRetake,
      questionIds: form.questionIds,
      questionScores,
    }
    if (editingPaperId.value) {
      await store.updateExam(editingPaperId.value, payload)
      ElMessage.success('试卷已更新')
    } else {
      await store.createExam(payload)
      ElMessage.success('试卷已保存为草稿')
    }
    await store.loadExams()
    drafts.value = store.exams.filter((e) => e.status === 'draft')
    resetForm()
  } catch (error) {
    const message = error instanceof Error ? error.message : '保存失败，请稍后重试'
    ElMessage.error(message)
  } finally {
    saving.value = false
  }
}

async function editPaper(exam: Exam) {
  const detail = await api.getExamDetailApi(exam.examId)
  editingPaperId.value = exam.examId
  form.examName = detail.exam.examName
  form.description = detail.exam.description || ''
  form.duration = detail.exam.duration || 60
  form.startTime = (detail.exam.startTime as string) ?? null
  form.endTime = (detail.exam.endTime as string) ?? null
  form.allowRetake = detail.exam.allowRetake
  form.questionIds = detail.questions.map((q) => q.questionId)
  if (detail.relationScores) {
    Object.entries(detail.relationScores).forEach(([questionId, score]) => {
      customScores[Number(questionId)] = score
    })
  }
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

async function deletePaper(exam: Exam) {
  try {
    await ElMessageBox.confirm(`确认删除试卷「${exam.examName}」吗？`, '删除试卷', { type: 'warning' })
    await store.deleteExam(exam.examId)
    drafts.value = drafts.value.filter((d) => d.examId !== exam.examId)
    ElMessage.success('试卷已删除')
  } catch { }
}
</script>

<template>
  <!-- 创建/编辑试卷 -->
  <el-card style="margin-bottom: 14px">
    <template #header>
      <div style="display: flex; align-items: center; justify-content: space-between">
        <strong>{{ editingPaperId ? '编辑试卷' : '新建试卷' }}</strong>
        <span style="display: flex; align-items: center; gap: 12px">
          <el-tag v-if="editingPaperId" type="warning" size="small">编辑模式</el-tag>
          <span style="color: var(--text-tertiary); font-size: 13px">已选 {{ form.questionIds.length }} 道题，预计 {{ selectedScore }} 分</span>
        </span>
      </div>
    </template>
    <el-form label-position="top">
      <div class="form-grid">
        <el-form-item label="试卷名称"><el-input v-model="form.examName" /></el-form-item>
        <el-form-item label="试卷说明"><el-input v-model="form.description" /></el-form-item>
        <el-form-item label="建议时长（分钟）"><el-input-number v-model="form.duration" :min="1" :max="180" style="width: 100%" /></el-form-item>
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
      <div style="display: flex; gap: 8px">
        <el-button type="primary" :loading="saving" @click="savePaper">{{ editingPaperId ? '保存修改' : '保存试卷' }}</el-button>
        <el-button v-if="editingPaperId" @click="resetForm">取消编辑</el-button>
      </div>

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
                <el-input-number :model-value="getScore(question)" :min="0" :max="100" :step="1" size="small" style="width: 90px" @update:model-value="(val: number) => setScore(question.questionId, val)" />
                <span style="font-size: 12px; color: var(--text-tertiary)">分</span>
                <el-button size="small" type="danger" link @click="form.questionIds = form.questionIds.filter((id) => id !== question.questionId)">移除</el-button>
              </div>
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>
    </el-form>
  </el-card>

  <!-- 选题弹窗 -->
  <el-dialog v-model="questionPickerVisible" title="选择题目" width="800px" top="5vh">
    <div style="display: flex; gap: 12px; margin-bottom: 14px; flex-wrap: wrap">
      <el-select v-model="pickerCategory" placeholder="按分类筛选" clearable style="width: 160px">
        <el-option v-for="item in categoryOptions" :key="item" :label="item" :value="item" />
      </el-select>
      <el-select v-model="pickerFileId" placeholder="按题库文件" clearable style="width: 180px">
        <el-option v-for="f in uploadFiles" :key="f.fileId" :label="f.fileName" :value="f.fileId" />
      </el-select>
      <el-input v-model="pickerSearch" placeholder="搜索题目内容" clearable style="width: 200px" />
      <span class="muted" style="font-size: 13px; line-height: 32px">显示 {{ pickerQuestions.length }} / {{ store.questions.length }} 道题</span>
      <el-checkbox
        :model-value="pickerAllSelected"
        :indeterminate="pickerIndeterminate"
        @change="(val: boolean) => togglePickerSelectAll(val)"
      >全选</el-checkbox>
    </div>
    <div style="max-height: 60vh; overflow-y: auto">
      <div
        v-for="(question, index) in pickerQuestions"
        :key="question.questionId"
        class="picker-item"
        :class="{ 'picker-item--selected': form.questionIds.includes(question.questionId) }"
        @click="
          form.questionIds.includes(question.questionId)
            ? form.questionIds = form.questionIds.filter((id) => id !== question.questionId)
            : form.questionIds.push(question.questionId)
        "
      >
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
      <span style="color: var(--text-tertiary); font-size: 13px">已选 {{ form.questionIds.length }} 道题</span>
      <el-button type="primary" @click="confirmQuestionSelection">确认选择</el-button>
    </template>
  </el-dialog>

  <!-- 试卷草稿列表 -->
  <el-card>
    <template #header>
      <div style="display: flex; align-items: center; justify-content: space-between">
        <strong>试卷草稿（{{ filteredDrafts.length }}）</strong>
        <el-input v-model="draftSearch" placeholder="搜索试卷名称..." clearable style="max-width: 220px" />
      </div>
    </template>
    <el-empty v-if="!store.exams.filter(e => e.status === 'draft').length" description="暂无试卷草稿，先新建一份试卷" />
    <el-empty v-else-if="!filteredDrafts.length && draftSearch" description="没有匹配的试卷" />
    <el-table v-else :data="filteredDrafts" stripe>
      <el-table-column prop="examName" label="试卷名称" min-width="180" />
      <el-table-column prop="description" label="说明" min-width="160">
        <template #default="{ row }">{{ row.description || '—' }}</template>
      </el-table-column>
      <el-table-column label="时长" width="80">
        <template #default="{ row }">{{ row.duration || '—' }} 分钟</template>
      </el-table-column>
      <el-table-column label="总分" width="70">
        <template #default="{ row }">{{ row.totalScore }} 分</template>
      </el-table-column>
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" plain @click="editPaper(row)">编辑</el-button>
          <el-button size="small" type="primary" @click="router.push({ path: '/admin/exams' })">去发布</el-button>
          <el-button size="small" type="danger" plain @click="deletePaper(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
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
  font-size: var(--text-body);
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.picker-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--text-small);
  color: var(--el-text-color-secondary);
}
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}
</style>
