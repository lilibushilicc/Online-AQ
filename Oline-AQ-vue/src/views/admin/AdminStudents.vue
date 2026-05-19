<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminLayout from './AdminLayout.vue'
import { useExamStore, type User } from '@/stores/exam'

const store = useExamStore()

const dialogVisible = ref(false)
const dialogTitle = ref('新增学生')
const editingUser = ref<User | null>(null)
const submitting = ref(false)

const form = reactive({
  username: '',
  password: '',
  realName: '',
  role: 'student' as 'admin' | 'student',
})

const students = ref<User[]>([])

onMounted(async () => {
  try {
    await store.loadUsers()
    students.value = store.users.filter((u) => u.role === 'student')
  } catch {
    ElMessage.error('加载学生数据失败，请刷新重试')
  }
})

function openCreate() {
  dialogTitle.value = '新增学生'
  editingUser.value = null
  form.username = ''
  form.password = ''
  form.realName = ''
  form.role = 'student'
  dialogVisible.value = true
}

function openEdit(user: User) {
  dialogTitle.value = '编辑学生'
  editingUser.value = user
  form.username = user.username
  form.password = ''
  form.realName = user.realName
  form.role = user.role
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.username || !form.realName) {
    ElMessage.warning('请填写账号和姓名')
    return
  }
  if (!editingUser.value && !form.password) {
    ElMessage.warning('请填写密码')
    return
  }

  submitting.value = true
  try {
    if (editingUser.value) {
      const payload: { realName: string; password?: string } = { realName: form.realName }
      if (form.password) {
        payload.password = form.password
      }
      await store.updateUser(editingUser.value.userId, payload)
      ElMessage.success('学生信息已更新')
    } else {
      await store.createUser({
        username: form.username,
        password: form.password,
        realName: form.realName,
        role: form.role,
      })
      ElMessage.success('学生已添加')
    }
    dialogVisible.value = false
    students.value = store.users.filter((u) => u.role === 'student')
  } catch (error) {
    const message = error instanceof Error ? error.message : '操作失败'
    ElMessage.error(message)
  } finally {
    submitting.value = false
  }
}

async function handleDelete(user: User) {
  try {
    await ElMessageBox.confirm(`确认删除学生「${user.realName}」(${user.username}) 吗？此操作不可恢复。`, '删除学生', {
      type: 'warning',
      confirmButtonText: '确认删除',
    })
    await store.deleteUser(user.userId)
    students.value = store.users.filter((u) => u.role === 'student')
    ElMessage.success('学生已删除')
  } catch {
    // cancelled
  }
}
</script>

<template>
  <AdminLayout title="学生管理" subtitle="管理学生账号，支持新增、编辑、删除，以及重置密码。">
    <el-card>
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px">
        <span style="color: var(--muted)">共 {{ students.length }} 名学生</span>
        <el-button type="primary" @click="openCreate">新增学生</el-button>
      </div>

      <el-table :data="students" stripe>
        <el-table-column prop="userId" label="ID" width="70" />
        <el-table-column prop="username" label="账号" min-width="140" />
        <el-table-column prop="realName" label="姓名" min-width="120" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px">
      <el-form label-position="top">
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
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </AdminLayout>
</template>
