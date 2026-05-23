<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useExamStore } from '@/stores/exam'
import * as api from '@/api'
import type { Question, UploadFileItem } from '@/types'
import { QUESTION_TYPE_LABEL } from '@/constants'
import { downloadFile } from '@/utils/download'

const route = useRoute()

const store = useExamStore()
const loading = ref(true)
const keyword = ref('')
const source = ref('')
const questionType = ref('')
const categoryFilter = ref('')
const selectedIds = ref<number[]>([])
const currentPage = ref(1)
const pageSize = ref(20)
const batchScore = ref(5)
const batchCategoryValue = ref('')
const uploadFiles = ref<UploadFileItem[]>([])
const activeFileId = ref<number | null>(null)
const editDialogVisible = ref(false)
const editForm = ref<Question>({
  questionId: 0,
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

const sourceOptions = computed(() => Array.from(new Set(store.questions.map((question) => String(question.sourceFileId ?? '手动新增')))))
const categoryOptions = computed(() => Array.from(new Set(store.questions.map((question) => question.category ?? '未分类'))))
const paginatedQuestions = computed(() => filteredQuestions.value.slice((currentPage.value - 1) * pageSize.value, currentPage.value * pageSize.value))
const totalFiltered = computed(() => filteredQuestions.value.length)
const filteredQuestions = computed(() => {
  const normalizedKeyword = keyword.value.trim().toLowerCase()
  return store.questions.filter((question) => {
    const matchKeyword =
      !normalizedKeyword ||
      question.questionContent.toLowerCase().includes(normalizedKeyword) ||
      question.correctAnswer.toLowerCase().includes(normalizedKeyword)
    const matchSource = !source.value || String(question.sourceFileId ?? '手动新增') === source.value
    const matchType = !questionType.value || question.questionType === questionType.value
    const matchCategory = !categoryFilter.value || (question.category ?? '未分类') === categoryFilter.value
    const matchFile = !activeFileId.value || question.sourceFileId === activeFileId.value
    return matchKeyword && matchSource && matchType && matchCategory && matchFile
  })
})
const activeFileName = computed(() => {
  if (!activeFileId.value) return '全部题目'
  return uploadFiles.value.find((f) => f.fileId === activeFileId.value)?.fileName ?? '全部题目'
})

onMounted(async () => {
  try {
    await Promise.all([store.loadQuestions(), store.loadCategories()])
    uploadFiles.value = await api.loadUploadFilesApi()
    const fileId = route.query.fileId
    if (fileId) {
      const id = Number(fileId)
      if (uploadFiles.value.some((f) => f.fileId === id)) {
        activeFileId.value = id
      }
    }
  } catch {
    ElMessage.error('加载数据失败，请刷新重试')
  } finally {
    loading.value = false
  }
})

function selectFile(fileId: number | null) {
  activeFileId.value = fileId
  selectedIds.value = []
  currentPage.value = 1
}

function onPageChange(page: number) { currentPage.value = page }
function onPageSizeChange(size: number) {
  pageSize.value = size
  currentPage.value = 1
}

async function updateSelectedCategory() {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先选择要设置分类的题目')
    return
  }
  if (!batchCategoryValue.value.trim()) {
    ElMessage.warning('请输入分类名称')
    return
  }
  await store.updateQuestionCategories(selectedIds.value, batchCategoryValue.value.trim())
  selectedIds.value = []
  ElMessage.success('批量设置分类完成')
}

function updateSelection(rows: Array<{ questionId: number }>) {
  selectedIds.value = rows.map((row) => row.questionId)
}

async function handleDeleteFile(fileId: number) {
  try {
    await ElMessageBox.confirm('确认删除此文件及其关联的题目？', '删除文件', { type: 'warning' })
  } catch { return }
  if (activeFileId.value === fileId) activeFileId.value = null
  await api.deleteFileApi(fileId)
  uploadFiles.value = await api.loadUploadFilesApi()
  await store.loadQuestions()
  ElMessage.success('文件及关联题目已删除')
}

async function deleteSelected() {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先选择要删除的题目')
    return
  }

  await ElMessageBox.confirm(`确认删除选中的 ${selectedIds.value.length} 道题目吗？`, '批量删除', { type: 'warning' })
  await store.deleteQuestions(selectedIds.value)
  selectedIds.value = []
  ElMessage.success('批量删除完成')
}

async function updateSelectedScore() {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先选择要设置分值的题目')
    return
  }

  await store.updateQuestionScores(selectedIds.value, batchScore.value)
  ElMessage.success('批量分值设置完成')
}

function openEditDialog(question: Question) {
  editForm.value = {
    questionId: question.questionId,
    questionContent: question.questionContent,
    questionType: question.questionType,
    optionA: question.optionA,
    optionB: question.optionB,
    optionC: question.optionC,
    optionD: question.optionD,
    correctAnswer: question.correctAnswer,
    score: Number(question.score),
    category: question.category ?? '',
    sourceFileId: question.sourceFileId,
  }
  editDialogVisible.value = true
}

async function saveEdit() {
  const payload = {
    questionContent: editForm.value.questionContent,
    questionType: editForm.value.questionType,
    correctAnswer: editForm.value.correctAnswer,
    score: Number(editForm.value.score),
    optionA: editForm.value.optionA,
    optionB: editForm.value.optionB,
    optionC: editForm.value.optionC,
    optionD: editForm.value.optionD,
    category: editForm.value.category ?? '',
  }
  await api.updateQuestionApi(editForm.value.questionId, payload)
  await Promise.all([store.loadQuestions(), store.loadCategories()])
  editDialogVisible.value = false
  ElMessage.success('题目已更新')
}
</script>

<template>
    <div v-loading="loading" class="question-bank-layout">
      <!-- 左侧文件面板 -->
      <aside class="file-sidebar">
        <div class="file-sidebar-header">
          <strong>文档列表</strong>
          <span class="muted" style="font-size: 12px">{{ uploadFiles.length }} 个文件</span>
        </div>
        <div
          class="file-item"
          :class="{ 'file-item--active': activeFileId === null }"
          @click="selectFile(null)"
        >
          <span>📂 全部题目</span>
          <span class="muted">{{ store.questions.length }}</span>
        </div>
        <div
          v-for="file in uploadFiles"
          :key="file.fileId"
          class="file-item"
          :class="{ 'file-item--active': activeFileId === file.fileId }"
          @click="selectFile(file.fileId)"
        >
          <div style="flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap">
            <span>📄 {{ file.fileName }}</span>
          </div>
          <span class="muted">{{ file.questionCount }}</span>
          <el-button size="small" type="danger" link @click.stop="handleDeleteFile(file.fileId)">删除</el-button>
        </div>
        <div v-if="uploadFiles.length === 0" class="muted" style="padding: 20px; text-align: center; font-size: 13px">
          暂无已解析文档
        </div>
      </aside>

      <!-- 右侧内容区 -->
      <div class="question-bank-main">
        <el-card>
          <div class="toolbar">
            <div class="toolbar-left">
              <el-input v-model="keyword" placeholder="搜索题干或答案" clearable style="width: 200px" />
              <el-select v-model="questionType" placeholder="题型" clearable style="width: 130px">
                <el-option label="单选题" value="single" />
                <el-option label="判断题" value="judge" />
                <el-option label="简答题" value="short_answer" />
                <el-option label="填空题" value="fill_blank" />
              </el-select>
              <el-select v-model="categoryFilter" placeholder="分类" clearable style="width: 150px">
                <el-option v-for="item in categoryOptions" :key="item" :label="item" :value="item" />
              </el-select>
              <el-select v-model="source" placeholder="来源文件" clearable style="width: 180px">
                <el-option v-for="item in sourceOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </div>
            <span class="toolbar-right">
              <span class="muted">共 {{ filteredQuestions.length }} / {{ store.questions.length }} 道题</span>
              <el-button size="small" plain @click="downloadFile('/questions/export', `题库导出_${new Date().toLocaleDateString()}.xlsx`, categoryFilter ? { category: categoryFilter } : undefined)">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14" style="margin-right: 4px"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
                导出
              </el-button>
            </span>
          </div>

          <div class="toolbar">
            <div class="toolbar-left">
              <el-tag v-if="activeFileId" closable @close="selectFile(null)" style="margin-right: 8px">{{ activeFileName }}</el-tag>
              <span class="batch-group">
                <el-input-number v-model="batchScore" :min="1" :max="100" style="width: 110px" size="small" />
                <el-button size="small" type="primary" plain @click="updateSelectedScore">批量设分</el-button>
              </span>
              <span class="batch-group">
                <el-input v-model="batchCategoryValue" placeholder="分类名称" clearable style="width: 130px" size="small" />
                <el-button size="small" type="success" plain @click="updateSelectedCategory">批量设分类</el-button>
              </span>
              <el-button size="small" type="danger" plain @click="deleteSelected">批量删除</el-button>
            </div>
            <span class="muted" style="font-size: 12px">已选 {{ selectedIds.length }} 道题</span>
          </div>

          <el-table :data="paginatedQuestions" stripe @selection-change="updateSelection">
            <el-table-column type="selection" width="50" />
            <el-table-column prop="questionId" label="ID" width="70" />
            <el-table-column prop="questionContent" label="题干" min-width="240" />
            <el-table-column label="题型" width="100">
              <template #default="{ row }">
                {{ QUESTION_TYPE_LABEL[row.questionType as keyof typeof QUESTION_TYPE_LABEL] || row.questionType }}
              </template>
            </el-table-column>
            <el-table-column prop="correctAnswer" label="答案" width="80" />
            <el-table-column prop="score" label="分值" width="70" />
            <el-table-column label="分类" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.category" size="small" effect="plain">{{ row.category }}</el-tag>
                <span v-else class="muted" style="font-size: 12px">未分类</span>
              </template>
            </el-table-column>
            <el-table-column prop="sourceFileId" label="来源" min-width="100" />
            <el-table-column label="操作" width="140">
              <template #default="{ row }">
                <el-button type="primary" link @click="openEditDialog(row)">编辑</el-button>
                <el-popconfirm title="确认删除此题？" @confirm="store.deleteQuestion(row.questionId)">
                  <template #reference>
                    <el-button type="danger" link>删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>

          <div style="display: flex; justify-content: center; margin-top: 16px" v-if="totalFiltered > pageSize">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :total="totalFiltered"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next"
              @current-change="onPageChange"
              @size-change="onPageSizeChange"
            />
          </div>
        </el-card>
      </div>
    </div>

    <el-dialog v-model="editDialogVisible" title="编辑题目" width="600px" :close-on-click-modal="false">
      <el-form :model="editForm" label-position="top">
        <el-form-item label="题目内容">
          <el-input v-model="editForm.questionContent" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="题型">
          <el-select v-model="editForm.questionType" style="width: 100%">
            <el-option label="单选题" value="single" />
            <el-option label="判断题" value="judge" />
            <el-option label="简答题" value="short_answer" />
            <el-option label="填空题" value="fill_blank" />
          </el-select>
        </el-form-item>
        <template v-if="editForm.questionType === 'single'">
          <el-form-item label="选项 A">
            <el-input v-model="editForm.optionA" />
          </el-form-item>
          <el-form-item label="选项 B">
            <el-input v-model="editForm.optionB" />
          </el-form-item>
          <el-form-item label="选项 C">
            <el-input v-model="editForm.optionC" />
          </el-form-item>
          <el-form-item label="选项 D">
            <el-input v-model="editForm.optionD" />
          </el-form-item>
        </template>
        <el-form-item label="正确答案">
          <el-input v-model="editForm.correctAnswer" />
        </el-form-item>
        <el-form-item label="分值">
          <el-input-number v-model="editForm.score" :min="1" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="editForm.category" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
</template>

<style scoped>
.question-bank-layout {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}
.file-sidebar {
  width: 220px;
  flex-shrink: 0;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color);
  border-radius: var(--el-border-radius-base);
  overflow: hidden;
}
.file-sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-bottom: 1px solid var(--el-border-color);
  background: var(--el-fill-color-lighter);
}
.file-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 14px;
  cursor: pointer;
  transition: background 0.15s;
  font-size: 13px;
  border-bottom: 1px solid var(--el-border-color-light);
}
.file-item:hover {
  background: var(--el-color-primary-light-9);
}
.file-item--active {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  font-weight: 600;
}
.question-bank-main {
  flex: 1;
  min-width: 0;
}
.batch-group {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
</style>
