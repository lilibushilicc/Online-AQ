<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading, Delete, Plus } from '@element-plus/icons-vue'
import { downloadFile } from '@/utils/download'
import StatCards from '@/views/components/StatCards.vue'
import { useExamStore, type WrongQuestionGroup, type WrongNotebook, type NotebookDetail } from '@/stores/exam'

const store = useExamStore()
const route = useRoute()
const router = useRouter()
const notebookId = computed(() => route.params.notebookId as string)
const isAllView = computed(() => notebookId.value === 'all')

const groups = ref<WrongQuestionGroup[]>([])
const notebook = ref<WrongNotebook | null>(null)
const notebookDetail = ref<NotebookDetail | null>(null)
const loading = ref(true)
const addDialogVisible = ref(false)
const addTargetAnswerId = ref<number | null>(null)
const addTargetQuestion = ref('')
const notebooks = ref<WrongNotebook[]>([])
const addNotebookId = ref<number | null>(null)
const adding = ref(false)

const typeLabel: Record<string, string> = {
  single: '单选',
  judge: '判断',
  short_answer: '简答',
  fill_blank: '填空',
}

async function loadData() {
  loading.value = true
  try {
    if (isAllView.value) {
      groups.value = await store.getWrongQuestions()
      notebooks.value = await store.loadNotebooks()
    } else {
      const detail = await store.getNotebookDetail(Number(notebookId.value))
      notebookDetail.value = detail
      groups.value = detail.groups || []
      notebooks.value = await store.loadNotebooks()
    }
  } finally {
    loading.value = false
  }
}

onMounted(loadData)

const totalQuestions = computed(() => groups.value.reduce((s, g) => s + g.questions.length, 0))

async function handleRemove(itemId: number) {
  try {
    await store.removeNotebookItem(Number(notebookId.value), itemId)
    await loadData()
  } catch {
    ElMessage.error('移除失败，请稍后重试')
  }
}

function openAddDialog(answerId: number, questionContent: string) {
  addTargetAnswerId.value = answerId
  addTargetQuestion.value = questionContent
  addNotebookId.value = notebooks.value.at(0)?.notebookId ?? null
  addDialogVisible.value = true
}

async function handleAddToNotebook() {
  if (!addTargetAnswerId.value || !addNotebookId.value) return
  adding.value = true
  try {
    await store.addToNotebook(addNotebookId.value, addTargetAnswerId.value)
    addDialogVisible.value = false
    await store.loadNotebooks().then(d => { notebooks.value = d })
  } finally {
    adding.value = false
  }
}

function handleExport() {
  if (isAllView.value) {
    downloadFile('/api/results/export/wrong', `错题导出_${new Date().toLocaleDateString()}.xlsx`)
  } else {
    downloadFile(`/api/wrong-notebooks/${notebookId.value}/export`, `错题本导出_${new Date().toLocaleDateString()}.xlsx`)
  }
}

async function handleDeleteNotebook() {
  if (isAllView.value) return
  try {
    await store.deleteNotebook(Number(notebookId.value))
    router.replace('/student/wrong-book')
  } catch {
    ElMessage.error('删除失败，请稍后重试')
  }
}
</script>

<template>
    <section v-if="!loading">
      <StatCards :columns="2" :items="[
        { title: '错题数量', value: totalQuestions, suffix: '道' },
        { title: '涉及考试', value: groups.length, suffix: '场' },
      ]" />

      <div v-if="groups.length === 0">
        <el-empty :description="isAllView ? '暂无错题，继续保持！' : '此错题本暂无内容，去全部错题中添加吧'" />
      </div>

      <el-card v-for="group in groups" :key="group.examId" style="margin-bottom: 14px">
        <template #header>
          <div style="display: flex; align-items: center; justify-content: space-between">
            <strong>{{ group.examName }}</strong>
            <el-button size="small" plain @click="router.push(`/student/exams/${group.examId}`)">查看考试</el-button>
          </div>
        </template>
        <div v-for="(item, idx) in group.questions" :key="item.answerId" style="margin-bottom: 14px; padding-bottom: 14px; border-bottom: 1px solid var(--el-border-color)" :style="idx === group.questions.length - 1 ? 'border-bottom: none; margin-bottom: 0; padding-bottom: 0' : ''">
          <div style="display: flex; align-items: flex-start; gap: 8px">
            <el-tag type="danger" style="flex-shrink: 0">错题 #{{ idx + 1 }}</el-tag>
            <div style="flex: 1; min-width: 0">
              <p style="font-weight: 600; margin-bottom: 8px">{{ item.questionContent }}</p>
              <div style="font-size: 13px">
                <el-tag size="small" style="margin-right: 6px">{{ typeLabel[item.questionType] || item.questionType }}</el-tag>
              </div>
              <div v-if="item.questionType === 'single' || item.questionType === 'judge'" class="option-grid" style="margin-top: 8px">
                <span :class="item.correctAnswer === 'A' ? 'correct-option' : ''">A. {{ item.optionA }}</span>
                <span :class="item.correctAnswer === 'B' ? 'correct-option' : ''">B. {{ item.optionB }}</span>
                <span v-if="item.optionC" :class="item.correctAnswer === 'C' ? 'correct-option' : ''">C. {{ item.optionC }}</span>
                <span v-if="item.optionD" :class="item.correctAnswer === 'D' ? 'correct-option' : ''">D. {{ item.optionD }}</span>
              </div>
              <div style="display: flex; flex-wrap: wrap; gap: 8px; margin-top: 10px; align-items: center">
                <el-tag type="danger" effect="plain">你的答案：{{ item.studentAnswer || '未作答' }}</el-tag>
                <el-tag type="success">正确答案：{{ item.correctAnswer }}</el-tag>
                <div style="flex: 1"></div>
                <el-button v-if="isAllView && notebooks.length > 0" size="small" type="primary" plain :icon="Plus" @click="openAddDialog(item.answerId, item.questionContent)">
                  加入错题本
                </el-button>
                <el-button v-if="!isAllView" size="small" type="danger" plain :icon="Delete" @click="handleRemove(item.answerId)">
                  移出
                </el-button>
              </div>
              <div style="margin-top: 4px; font-size: 12px; color: var(--muted)">
                提交时间：{{ new Date(item.submitTime).toLocaleString() }}
              </div>
            </div>
          </div>
        </div>
      </el-card>

      <!-- Add to Notebook Dialog -->
      <el-dialog v-model="addDialogVisible" title="添加到错题本" width="420px">
        <p style="margin-bottom: 12px; font-size: 13px; color: var(--ink-secondary)">选择目标错题本：</p>
        <el-select v-model="addNotebookId" placeholder="选择错题本" style="width: 100%">
          <el-option v-for="nb in notebooks" :key="nb.notebookId" :label="`${nb.notebookName} (${nb.itemCount}道)`" :value="nb.notebookId" />
        </el-select>
        <template #footer>
          <el-button @click="addDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="adding" :disabled="!addNotebookId" @click="handleAddToNotebook">确认添加</el-button>
        </template>
      </el-dialog>
    </section>

    <div v-else style="text-align: center; padding: 60px 0">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      <p style="margin-top: 12px; color: var(--muted)">加载中...</p>
    </div>
</template>

<style scoped>
.option-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
  font-size: 13px;
}
.correct-option {
  color: var(--el-color-success);
  font-weight: 600;
}
</style>
