<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminLayout from './AdminLayout.vue'
import { useExamStore } from '@/stores/exam'

const store = useExamStore()
const keyword = ref('')
const source = ref('')
const questionType = ref('')
const categoryFilter = ref('')
const selectedIds = ref<number[]>([])
const batchScore = ref(5)
const batchCategoryValue = ref('')

const sourceOptions = computed(() => Array.from(new Set(store.questions.map((question) => String(question.sourceFileId ?? '手动新增')))))
const categoryOptions = computed(() => Array.from(new Set(store.questions.map((question) => question.category ?? '未分类'))))
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
    return matchKeyword && matchSource && matchType && matchCategory
  })
})

onMounted(() => { store.loadQuestions(); store.loadCategories() })

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
</script>

<template>
  <AdminLayout title="题库管理" subtitle="支持按关键词、题型、来源筛选题目，并进行批量删除与批量分值设置。">
    <el-card>
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input v-model="keyword" placeholder="搜索题干或答案" clearable style="width: 200px" />
          <el-select v-model="questionType" placeholder="题型" clearable style="width: 130px">
            <el-option label="单选题" value="single" />
            <el-option label="判断题" value="judge" />
          </el-select>
          <el-select v-model="categoryFilter" placeholder="分类" clearable style="width: 150px">
            <el-option v-for="item in categoryOptions" :key="item" :label="item" :value="item" />
          </el-select>
          <el-select v-model="source" placeholder="来源文件" clearable style="width: 180px">
            <el-option v-for="item in sourceOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </div>
        <span class="muted">共 {{ filteredQuestions.length }} / {{ store.questions.length }} 道题</span>
      </div>

      <div class="toolbar">
        <div class="toolbar-left">
          <el-input-number v-model="batchScore" :min="1" :max="100" style="width: 120px" />
          <el-button type="primary" plain @click="updateSelectedScore">批量设分</el-button>
          <el-input v-model="batchCategoryValue" placeholder="分类名称" clearable style="width: 140px" />
          <el-button type="success" plain @click="updateSelectedCategory">批量设分类</el-button>
          <el-button type="danger" plain @click="deleteSelected">批量删除</el-button>
        </div>
        <span class="muted">已选 {{ selectedIds.length }} 道题</span>
      </div>

      <el-table :data="filteredQuestions" stripe @selection-change="updateSelection">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="questionId" label="ID" width="70" />
        <el-table-column prop="questionContent" label="题干" min-width="240" />
        <el-table-column prop="questionType" label="题型" width="100" />
        <el-table-column prop="correctAnswer" label="答案" width="80" />
        <el-table-column prop="score" label="分值" width="70" />
        <el-table-column label="分类" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.category" size="small" effect="plain">{{ row.category }}</el-tag>
            <span v-else class="muted" style="font-size: 12px">未分类</span>
          </template>
        </el-table-column>
        <el-table-column prop="sourceFileId" label="来源" min-width="100" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="danger" link @click="store.deleteQuestion(row.questionId)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </AdminLayout>
</template>
