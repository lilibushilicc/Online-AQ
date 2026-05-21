<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { InfoFilled, Edit, Delete, Check, User, Search } from '@element-plus/icons-vue'
import { useExamStore, type User as UserType } from '@/stores/exam'
import { formatTime } from '@/utils/format'
import * as api from '@/api'

const store = useExamStore()

const students = ref<UserType[]>([])
const loading = ref(true)
const studentSearch = ref('')
const filteredStudents = computed(() => {
  if (!studentSearch.value) return students.value
  const kw = studentSearch.value.toLowerCase()
  return students.value.filter((s) =>
    s.realName.toLowerCase().includes(kw) || s.username.toLowerCase().includes(kw)
  )
})

const createEditVisible = ref(false)
const createEditTitle = ref('新增学生')
const editingUser = ref<UserType | null>(null)
const submitting = ref(false)

const form = reactive({ username: '', password: '', realName: '' })

// ------ 学生详情（同款个人中心布局）------
const detailVisible = ref(false)
const detailUser = ref<UserType | null>(null)
const detailForm = ref({ realName: '', email: '', password: '' })
const emailError = ref('')
const savingDetail = ref(false)

onMounted(async () => {
  try {
    await store.loadUsers()
    students.value = store.users.filter((u) => u.role === 'student')
  } catch {
    ElMessage.error('加载学生数据失败')
  } finally {
    loading.value = false
  }
})

function refreshList() {
  students.value = store.users.filter((u) => u.role === 'student')
}

function getInitial(name: string) { return name ? name.charAt(0) : '?' }

// ------ 新增/快速编辑 ------
function openCreate() {
  createEditTitle.value = '新增学生'
  editingUser.value = null
  form.username = ''; form.password = ''; form.realName = ''
  createEditVisible.value = true
}

function openQuickEdit(user: UserType) {
  createEditTitle.value = '编辑学生'
  editingUser.value = user
  form.username = user.username
  form.password = ''
  form.realName = user.realName
  createEditVisible.value = true
}

async function handleSubmit() {
  if (!form.username || !form.realName) { ElMessage.warning('请填写账号和姓名'); return }
  if (!editingUser.value && !form.password) { ElMessage.warning('请填写密码'); return }
  submitting.value = true
  try {
    if (editingUser.value) {
      await store.updateUser(editingUser.value.userId, { realName: form.realName, ...(form.password ? { password: form.password } : {}) })
      ElMessage.success('学生信息已更新')
    } else {
      await store.createUser({ username: form.username, password: form.password, realName: form.realName, role: 'student' })
      ElMessage.success('学生已添加')
    }
    createEditVisible.value = false
    refreshList()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  } finally {
    submitting.value = false
  }
}

// ------ 学生详情（同款个人中心布局）------
async function openDetail(user: UserType) {
  try {
    detailUser.value = await api.getUserApi(user.userId)
  } catch {
    detailUser.value = user
  }
  detailForm.value = {
    realName: detailUser.value.realName,
    email: detailUser.value.email ?? '',
    password: '',
  }
  emailError.value = ''
  detailVisible.value = true
}

function validateEmail(email: string) {
  if (!email) { emailError.value = ''; return true }
  if (!/^[\w.-]+@[\w.-]+\.\w{2,}$/.test(email)) { emailError.value = '邮箱格式不正确'; return false }
  emailError.value = ''; return true
}

async function saveDetail() {
  if (!detailForm.value.realName.trim()) { ElMessage.warning('姓名不能为空'); return }
  if (!validateEmail(detailForm.value.email)) return

  savingDetail.value = true
  try {
    const payload: { realName: string; email: string; password?: string } = {
      realName: detailForm.value.realName.trim(),
      email: detailForm.value.email.trim(),
    }
    if (detailForm.value.password) {
      if (detailForm.value.password.length < 6) { ElMessage.warning('密码至少6位'); savingDetail.value = false; return }
      payload.password = detailForm.value.password
    }
    await store.updateUser(detailUser.value!.userId, payload)
    const updated = await api.getUserApi(detailUser.value!.userId)
    detailUser.value = updated
    ElMessage.success('学生信息已更新')
    refreshList()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '修改失败')
  } finally {
    savingDetail.value = false
  }
}

// ------ 删除 ------
async function handleDelete(user: UserType) {
  try {
    await ElMessageBox.confirm(`确认删除学生「${user.realName}」(${user.username}) 吗？`, '删除学生', { type: 'warning', confirmButtonText: '确认删除' })
    await store.deleteUser(user.userId)
    refreshList()
    ElMessage.success('学生已删除')
  } catch { }
}
</script>

<template>
  <div>
    <el-card>
      <div class="students-header">
        <div style="display: flex; align-items: center; gap: 12px">
          <span style="color: var(--muted)">共 {{ students.length }} 名学生</span>
          <el-input v-model="studentSearch" placeholder="搜索姓名或账号..." clearable style="max-width: 220px" :prefix-icon="Search" />
        </div>
        <el-button type="primary" @click="openCreate">新增学生</el-button>
      </div>

      <!-- 骨架加载 -->
      <template v-if="loading">
        <div class="skeleton-table">
          <div v-for="n in 6" :key="n" class="skeleton-table-row">
            <div class="skeleton-block skeleton-col-id"></div>
            <div class="skeleton-block skeleton-col-username"></div>
            <div class="skeleton-block skeleton-col-name"></div>
            <div class="skeleton-block skeleton-col-email"></div>
            <div class="skeleton-block skeleton-col-time"></div>
            <div class="skeleton-block skeleton-col-actions"></div>
          </div>
        </div>
      </template>

      <!-- 空状态 -->
      <div v-else-if="!students.length" class="table-empty-state fade-in">
        <div class="empty-icon">
          <el-icon :size="20"><User /></el-icon>
        </div>
        <p>暂无学生数据</p>
        <div class="empty-action">
          <el-button type="primary" @click="openCreate">新增学生</el-button>
        </div>
      </div>

      <!-- 无搜索结果 -->
      <div v-else-if="!filteredStudents.length && studentSearch" class="table-empty-state fade-in">
        <div class="empty-icon"><el-icon :size="20"><Search /></el-icon></div>
        <p>没有匹配的学生</p>
      </div>

      <!-- 表格 -->
      <el-table v-else :data="filteredStudents" stripe>
        <el-table-column prop="userId" label="ID" width="70" />
        <el-table-column prop="username" label="账号" min-width="120" />
        <el-table-column prop="realName" label="姓名" min-width="100" />
        <el-table-column prop="email" label="邮箱" min-width="180">
          <template #default="{ row }">{{ row.email || '—' }}</template>
        </el-table-column>
        <el-table-column label="注册时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :icon="InfoFilled" @click="openDetail(row)">详情</el-button>
            <el-button type="primary" link :icon="Edit" @click="openQuickEdit(row)">编辑</el-button>
            <el-button type="danger" link :icon="Edit" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/快速编辑弹窗 -->
    <el-dialog v-model="createEditVisible" :title="createEditTitle" width="460px">
      <el-form label-position="top" @submit.prevent>
        <el-form-item label="账号">
          <el-input v-model="form.username" :disabled="!!editingUser" placeholder="登录账号" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.realName" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item :label="editingUser ? '新密码（留空不修改）' : '密码'">
          <el-input v-model="form.password" type="password" show-password placeholder="登录密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createEditVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 学生详情弹窗 - 同款个人中心布局 -->
    <el-dialog v-model="detailVisible" title="学生详情" width="640px" :close-on-click-modal="false" top="5vh">
      <template v-if="detailUser">
        <div class="student-profile-card">
          <div class="sp-hero">
            <div class="sp-avatar">{{ getInitial(detailUser.realName) }}</div>
            <div class="sp-hero-text">
              <div class="sp-name">
                {{ detailUser.realName }}
                <el-tag type="success" effect="light" size="small" style="margin-left: 12px;">学生</el-tag>
              </div>
              <div class="sp-account">@{{ detailUser.username }}</div>
            </div>
          </div>

          <el-divider style="margin: 24px 0 20px" />

          <div class="sp-info-grid">
            <div class="sp-info-row">
              <span class="sp-label">账号</span>
              <span class="sp-value readonly">{{ detailUser.username }}</span>
            </div>

            <div class="sp-info-row editing">
              <span class="sp-label">姓名</span>
              <el-input v-model="detailForm.realName" placeholder="真实姓名" maxlength="20" show-word-limit />
            </div>

            <div class="sp-info-row editing">
              <span class="sp-label">邮箱</span>
              <el-input v-model="detailForm.email" placeholder="邮箱（选填）" @input="validateEmail(detailForm.email)" />
              <div v-if="emailError" class="sp-field-error">{{ emailError }}</div>
            </div>

            <div class="sp-info-row">
              <span class="sp-label">身份</span>
              <span class="sp-value readonly">学生</span>
            </div>

            <div class="sp-info-row">
              <span class="sp-label">注册时间</span>
              <span class="sp-value readonly">{{ formatTime(detailUser.createTime) }}</span>
            </div>

            <div class="sp-info-row editing">
              <span class="sp-label">密码</span>
              <el-input v-model="detailForm.password" type="password" show-password placeholder="输入新密码（留空不修改）" clearable />
              <div class="sp-field-hint">留空则保持原密码不变</div>
            </div>
          </div>
        </div>
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" :icon="Check" :loading="savingDetail" @click="saveDetail">保存修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.students-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

/* 同款个人中心布局 */
.student-profile-card {
  background: var(--paper);
  border-radius: 12px;
  padding: 28px 32px;
}

.sp-hero {
  display: flex;
  align-items: center;
  gap: 20px;
}

.sp-avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  flex-shrink: 0;
  background: linear-gradient(135deg, #529b2e, #85ce61);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 700;
  color: #fff;
}

.sp-hero-text {
  min-width: 0;
}

.sp-name {
  font-size: 22px;
  font-weight: 700;
  color: var(--ink);
  line-height: 1.4;
}

.sp-account {
  font-size: 14px;
  color: var(--muted);
  margin-top: 4px;
}

.sp-info-grid {
  display: flex;
  flex-direction: column;
}

.sp-info-row {
  display: flex;
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px solid var(--line-light);
  min-height: 48px;
  flex-wrap: wrap;
  gap: 8px;
}

.sp-info-row:last-child {
  border-bottom: none;
}

.sp-label {
  width: 80px;
  flex-shrink: 0;
  font-size: 14px;
  color: var(--ink-tertiary);
  font-weight: 500;
}

.sp-value {
  font-size: 14px;
  color: var(--ink);
}

.sp-value.readonly {
  color: var(--ink-tertiary);
}

.sp-info-row .el-input {
  flex: 1;
  min-width: 180px;
}

.sp-field-error {
  width: 100%;
  font-size: 12px;
  color: var(--red);
  margin-top: -4px;
}

.sp-field-hint {
  width: 100%;
  font-size: 12px;
  color: var(--muted);
  margin-top: -4px;
}

/* Skeleton */
.skeleton-table {
  padding: 8px 0;
}

.skeleton-table-row {
  display: flex;
  align-items: center;
  padding: 15px 0;
  border-bottom: 1px solid var(--line-light);
  gap: 16px;
}

.skeleton-col-id {
  width: 50px;
  height: 14px;
}

.skeleton-col-username {
  width: 100px;
  height: 14px;
}

.skeleton-col-name {
  width: 80px;
  height: 14px;
}

.skeleton-col-email {
  width: 160px;
  height: 14px;
}

.skeleton-col-time {
  width: 120px;
  height: 14px;
}

.skeleton-col-actions {
  width: 200px;
  height: 14px;
  margin-left: auto;
}
</style>
