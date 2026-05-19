<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import StudentLayout from './StudentLayout.vue'
import { Loading, Plus, Notebook, Delete, Edit } from '@element-plus/icons-vue'
import StatCards from '@/views/components/StatCards.vue'
import { useExamStore, type WrongNotebook, type WrongQuestionGroup } from '@/stores/exam'

const store = useExamStore()
const router = useRouter()
const notebooks = ref<WrongNotebook[]>([])
const allWrongCount = ref(0)
const loading = ref(true)
const createDialogVisible = ref(false)
const editDialogVisible = ref(false)
const newName = ref('')
const newDesc = ref('')
const editNotebook = ref<WrongNotebook | null>(null)
const editName = ref('')
const editDesc = ref('')
const submitting = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const [nbData, wrongData] = await Promise.all([
      store.loadNotebooks(),
      store.getWrongQuestions()
    ])
    notebooks.value = nbData
    allWrongCount.value = wrongData.reduce((s: number, g: WrongQuestionGroup) => s + g.questions.length, 0)
  } finally {
    loading.value = false
  }
})

async function handleCreate() {
  if (!newName.value.trim()) return
  submitting.value = true
  try {
    await store.createNotebook(newName.value.trim(), newDesc.value.trim())
    createDialogVisible.value = false
    newName.value = ''
    newDesc.value = ''
    notebooks.value = await store.loadNotebooks()
  } finally {
    submitting.value = false
  }
}

function openEdit(notebook: WrongNotebook) {
  editNotebook.value = notebook
  editName.value = notebook.notebookName
  editDesc.value = notebook.description || ''
  editDialogVisible.value = true
}

async function handleEdit() {
  if (!editName.value.trim() || !editNotebook.value) return
  submitting.value = true
  try {
    await store.updateNotebook(editNotebook.value.notebookId, editName.value.trim(), editDesc.value.trim())
    editDialogVisible.value = false
    notebooks.value = await store.loadNotebooks()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(notebook: WrongNotebook) {
  try {
    await store.deleteNotebook(notebook.notebookId)
    notebooks.value = await store.loadNotebooks()
  } catch {
    ElMessage.error('删除失败，请稍后重试')
  }
}
</script>

<template>
  <StudentLayout title="错题本" subtitle="管理多个错题本，分类整理错题，高效复习">
    <section v-if="!loading">
      <StatCards :columns="3" :items="[
        { title: '错题本数量', value: notebooks.length, suffix: '个' },
        { title: '全部错题', value: allWrongCount, suffix: '道' },
        { title: '已分类错题', value: notebooks.reduce((s, n) => s + n.itemCount, 0), suffix: '道' },
      ]" />

      <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px">
        <el-button type="primary" :icon="Plus" @click="createDialogVisible = true">新建错题本</el-button>
      </div>

      <el-row :gutter="14">
        <el-col :xs="24" :sm="12" :lg="8" style="margin-bottom: 14px">
          <el-card shadow="hover" class="notebook-card all-wrong-card" @click="router.push('/student/wrong-book/all')">
            <div class="nb-icon-wrap all-icon">
              <el-icon :size="22"><Notebook /></el-icon>
            </div>
            <h3>全部错题</h3>
            <p>查看所有考试中的错题，按考试分组展示</p>
            <div class="nb-meta">
              <span class="nb-count">{{ allWrongCount }} 道错题</span>
            </div>
          </el-card>
        </el-col>

        <el-col v-for="nb in notebooks" :key="nb.notebookId" :xs="24" :sm="12" :lg="8" style="margin-bottom: 14px">
          <el-card shadow="hover" class="notebook-card" @click="router.push(`/student/wrong-book/${nb.notebookId}`)">
            <div class="nb-icon-wrap">
              <el-icon :size="22"><Notebook /></el-icon>
            </div>
            <h3>{{ nb.notebookName }}</h3>
            <p>{{ nb.description || '暂无描述' }}</p>
            <div class="nb-meta">
              <span class="nb-count">{{ nb.itemCount }} 道错题</span>
              <span class="nb-actions" @click.stop>
                <el-button link type="primary" :icon="Edit" size="small" @click="openEdit(nb)">编辑</el-button>
                <el-popconfirm title="确定删除此错题本？错题本中的题目不会被删除。" @confirm="handleDelete(nb)">
                  <template #reference>
                    <el-button link type="danger" :icon="Delete" size="small">删除</el-button>
                  </template>
                </el-popconfirm>
              </span>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <div v-if="notebooks.length === 0" style="text-align: center; padding: 40px 0; color: var(--muted)">
        还没有创建错题本，点击"新建错题本"开始整理错题
      </div>

      <!-- Create Dialog -->
      <el-dialog v-model="createDialogVisible" title="新建错题本" width="440px" :close-on-click-modal="false">
        <el-form label-position="top">
          <el-form-item label="错题本名称" required>
            <el-input v-model="newName" placeholder="例如：Java基础错题" maxlength="50" show-word-limit />
          </el-form-item>
          <el-form-item label="描述（选填）">
            <el-input v-model="newDesc" type="textarea" :rows="2" placeholder="简要描述此错题本的用途" maxlength="200" show-word-limit />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="createDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleCreate">创建</el-button>
        </template>
      </el-dialog>

      <!-- Edit Dialog -->
      <el-dialog v-model="editDialogVisible" title="编辑错题本" width="440px" :close-on-click-modal="false">
        <el-form label-position="top">
          <el-form-item label="错题本名称" required>
            <el-input v-model="editName" placeholder="错题本名称" maxlength="50" show-word-limit />
          </el-form-item>
          <el-form-item label="描述">
            <el-input v-model="editDesc" type="textarea" :rows="2" placeholder="简要描述" maxlength="200" show-word-limit />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleEdit">保存</el-button>
        </template>
      </el-dialog>
    </section>

    <div v-else style="text-align: center; padding: 60px 0">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      <p style="margin-top: 12px; color: var(--muted)">加载中...</p>
    </div>
  </StudentLayout>
</template>

<style scoped>
.notebook-card {
  cursor: pointer;
  transition: all 0.25s var(--ease-out);
  min-height: 170px;
  display: flex;
  flex-direction: column;
}
.notebook-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg) !important;
}
.all-wrong-card {
  border: 1px solid var(--ink-green-light) !important;
}
.nb-icon-wrap {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-sm);
  background: var(--accent-light);
  color: var(--ink-green);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}
.all-icon {
  background: rgba(26, 61, 46, 0.12);
  color: var(--ink-green);
}
.notebook-card h3 {
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 600;
  color: var(--ink);
}
.notebook-card p {
  margin: 0;
  font-size: 13px;
  color: var(--muted);
  flex: 1;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.nb-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid var(--line-light);
}
.nb-count {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink-green);
}
.nb-actions {
  display: flex;
  gap: 2px;
}
</style>
