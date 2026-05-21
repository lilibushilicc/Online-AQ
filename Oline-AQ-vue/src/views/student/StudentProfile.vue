<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Edit, Check, Close, Key } from '@element-plus/icons-vue'
import * as api from '@/api'
import { useExamStore } from '@/stores/exam'

const store = useExamStore()

import type { User } from '@/types'
const profile = ref<User>({ userId: 0, username: '', realName: '', role: 'student' })
const loading = ref(true)
const editMode = ref(false)
const saving = ref(false)

const editForm = ref({ realName: '', email: '', password: '' })
const emailError = ref('')

const pwdDialogVisible = ref(false)
const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const savingPwd = ref(false)

function getInitial(name: string) { return name ? name.charAt(0) : '?' }
function isAdmin() { return profile.value.role === 'admin' }

onMounted(loadProfile)

async function loadProfile() {
  loading.value = true
  try {
    const data = await api.getMyProfileApi()
    profile.value = data
  } catch {
    ElMessage.error('加载个人信息失败')
  } finally {
    loading.value = false
  }
}

function syncStore(data: { realName: string; email?: string }) {
  if (store.currentUser) {
    store.currentUser.realName = data.realName
    store.currentUser.email = data.email ?? ''
    localStorage.setItem('user', JSON.stringify(store.currentUser))
  }
}

function enterEditMode() {
  editForm.value = {
    realName: profile.value.realName,
    email: profile.value.email ?? '',
    password: '',
  }
  emailError.value = ''
  editMode.value = true
}

function cancelEdit() {
  editMode.value = false
  emailError.value = ''
}

function validateEmail(email: string) {
  if (!email) { emailError.value = ''; return true }
  if (!/^[\w.-]+@[\w.-]+\.\w{2,}$/.test(email)) {
    emailError.value = '邮箱格式不正确'; return false
  }
  emailError.value = ''; return true
}

async function saveEdit() {
  if (!editForm.value.realName.trim()) { ElMessage.warning('姓名不能为空'); return }
  if (!validateEmail(editForm.value.email)) return

  saving.value = true
  try {
    const payload: { realName: string; email: string; password?: string } = {
      realName: editForm.value.realName.trim(),
      email: editForm.value.email.trim(),
    }
    if (editForm.value.password) {
      if (editForm.value.password.length < 6) { ElMessage.warning('密码至少6位'); saving.value = false; return }
      payload.password = editForm.value.password
    }
    const data = await api.updateMyProfileApi(payload)
    profile.value.realName = data.realName
    profile.value.email = data.email ?? ''
    syncStore(data)
    ElMessage.success('个人资料已更新')
    editMode.value = false
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '修改失败')
  } finally {
    saving.value = false
  }
}

function formatTime(t: string | undefined) {
  if (!t) return '—'
  return t.replace('T', ' ').substring(0, 19)
}

async function changePassword() {
  if (!pwdForm.value.oldPassword) { ElMessage.warning('请输入旧密码'); return }
  if (!pwdForm.value.newPassword) { ElMessage.warning('请输入新密码'); return }
  if (pwdForm.value.newPassword.length < 6) { ElMessage.warning('新密码至少6位'); return }
  if (pwdForm.value.newPassword !== pwdForm.value.confirmPassword) { ElMessage.warning('两次密码不一致'); return }
  savingPwd.value = true
  try {
    await api.updateMyProfileApi({ oldPassword: pwdForm.value.oldPassword, newPassword: pwdForm.value.newPassword })
    ElMessage.success('密码修改成功，请重新登录')
    pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
    pwdDialogVisible.value = false
    setTimeout(() => store.logout(), 1000)
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '修改失败')
  } finally {
    savingPwd.value = false
  }
}
</script>

<template>
  <div class="profile-page">
    <!-- 骨架加载 -->
    <template v-if="loading">
      <div class="skeleton-page-title skeleton-block"></div>
      <div class="profile-card">
        <div class="profile-hero">
          <div class="skeleton-avatar"></div>
          <div class="skeleton-hero-info">
            <div class="skeleton-block skeleton-line"></div>
            <div class="skeleton-block skeleton-line--short"></div>
          </div>
        </div>
        <el-divider style="margin: 24px 0 20px" />
        <div class="skeleton-info-grid">
          <div v-for="n in 5" :key="n" class="skeleton-info-row">
            <div class="skeleton-block skeleton-label"></div>
            <div class="skeleton-block skeleton-line"></div>
          </div>
        </div>
        <div class="skeleton-actions">
          <div class="skeleton-block skeleton-btn"></div>
          <div class="skeleton-block skeleton-btn skeleton-btn--wide"></div>
        </div>
      </div>
    </template>

    <!-- 实际内容 -->
    <template v-else>
      <h2 class="page-title">个人中心</h2>

      <div class="profile-card fade-in">
        <div class="profile-hero">
          <div class="avatar-circle" :class="isAdmin() ? 'avatar-admin' : 'avatar-student'">
            <span class="avatar-text">{{ getInitial(profile.realName) }}</span>
          </div>
          <div class="hero-text">
            <div class="hero-name">
              {{ profile.realName || profile.username }}
              <el-tag :type="isAdmin() ? 'warning' : 'success'" effect="light" size="small" style="margin-left: 12px;">
                {{ isAdmin() ? '管理员' : '学生' }}
              </el-tag>
            </div>
            <div class="hero-account">@{{ profile.username }}</div>
          </div>
        </div>

        <el-divider style="margin: 24px 0 20px" />

        <div class="info-section">
          <div class="info-grid stagger-group">
            <div class="info-row">
              <span class="info-label">账号</span>
              <span class="info-value readonly">{{ profile.username }}</span>
            </div>

            <div class="info-row" :class="{ editing: editMode }">
              <span class="info-label">姓名</span>
              <template v-if="editMode">
                <el-input v-model="editForm.realName" placeholder="真实姓名" maxlength="20" show-word-limit />
              </template>
              <template v-else>
                <span class="info-value">{{ profile.realName }}</span>
              </template>
            </div>

            <div class="info-row" :class="{ editing: editMode }">
              <span class="info-label">邮箱</span>
              <template v-if="editMode">
                <el-input v-model="editForm.email" placeholder="邮箱（选填）" :error="!!emailError" @input="validateEmail(editForm.email)" />
                <div v-if="emailError" class="field-error">{{ emailError }}</div>
              </template>
              <template v-else>
                <span class="info-value">{{ profile.email || '—' }}</span>
              </template>
            </div>

            <div class="info-row">
              <span class="info-label">身份</span>
              <span class="info-value readonly">{{ isAdmin() ? '管理员' : '学生' }}</span>
            </div>

            <div class="info-row">
              <span class="info-label">注册时间</span>
              <span class="info-value readonly">{{ formatTime(profile.createTime) }}</span>
            </div>

            <transition name="edit">
              <div v-if="editMode" class="info-row editing">
                <span class="info-label">密码</span>
                <el-input v-model="editForm.password" type="password" show-password placeholder="留空不修改" clearable />
                <div class="field-hint">留空则保持原密码不变</div>
              </div>
            </transition>
          </div>
        </div>

        <div class="profile-actions fade-in stagger-6">
          <template v-if="editMode">
            <el-button type="primary" :icon="Check" :loading="saving" @click="saveEdit">保存</el-button>
            <el-button :icon="Close" @click="cancelEdit">取消</el-button>
          </template>
          <template v-else>
            <el-button type="primary" :icon="Edit" @click="enterEditMode">编辑资料</el-button>
            <el-button :icon="Key" type="warning" plain @click="pwdDialogVisible = true">修改密码</el-button>
          </template>
        </div>
      </div>
    </template>

    <el-dialog v-model="pwdDialogVisible" title="修改密码" width="440px" :close-on-click-modal="false">
      <el-form label-position="top" @submit.prevent>
        <el-form-item label="旧密码">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入旧密码" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="至少6位" />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingPwd" @click="changePassword">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.profile-page { max-width: 780px; margin: 0 auto; padding: 28px; }

.page-title {
  font-size: var(--text-page-title); font-weight: 700;
  color: var(--text-primary); margin: 0 0 28px;
  font-family: var(--font-sans);
}

.profile-card {
  background: var(--bg-base); border-radius: 16px; padding: 40px 48px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-subtle);
  position: relative;
  transition: box-shadow var(--duration) var(--ease-out), transform var(--duration) var(--ease-out);
}

.profile-card:hover {
  box-shadow: var(--shadow-md);
}

/* 顶部装饰线 */
.profile-card::before {
  content: '';
  position: absolute;
  top: 0; left: 48px; right: 48px;
  height: 3px;
  border-radius: 0 0 3px 3px;
  background: linear-gradient(90deg, var(--accent-blue), var(--accent-blue), transparent);
}

.profile-hero { display: flex; align-items: center; gap: 28px; }

.avatar-circle {
  width: 88px; height: 88px; border-radius: 50%; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  position: relative;
  box-shadow: 0 0 0 3px var(--bg-base), var(--shadow-sm);
  will-change: transform;
  transition: transform var(--duration-fast) var(--ease-out);
}

.avatar-circle:hover {
  transform: scale(1.04);
}

.avatar-admin { background: linear-gradient(135deg, #e6a23c, #f0c87a); }
.avatar-student { background: linear-gradient(135deg, #529b2e, #85ce61); }
.avatar-text { font-size: 34px; font-weight: 700; color: #fff; }

.hero-text { min-width: 0; }
.hero-name {
  font-size: 24px; font-weight: 700;
  color: var(--text-primary); line-height: 1.4;
  display: flex; align-items: center; flex-wrap: wrap; gap: 8px;
}
.hero-account {
  font-size: var(--text-body); color: var(--text-tertiary); margin-top: 5px;
}

.info-grid { display: flex; flex-direction: column; }

.info-row {
  display: flex; align-items: center; padding: 16px 0;
  border-bottom: 1px solid var(--border-subtle); min-height: 52px;
  transition: padding var(--duration-fast), border-color var(--duration-fast);
}

.info-row:last-child { border-bottom: none; }
.info-row.editing { flex-wrap: wrap; gap: 10px; padding: 18px 0; }

.info-label {
  width: 110px; flex-shrink: 0; font-size: var(--text-body);
  color: var(--text-secondary); font-weight: 600;
  letter-spacing: 0.1px;
}

.info-value { font-size: var(--text-body); color: var(--text-primary); }
.info-value.readonly { color: var(--text-tertiary); }
.info-row.editing .el-input { flex: 1; min-width: 220px; }
.field-error { width: 100%; font-size: var(--text-small); color: var(--accent-red); margin-top: -4px; }
.field-hint { width: 100%; font-size: var(--text-small); color: var(--text-tertiary); margin-top: -4px; }

.profile-actions {
  display: flex; gap: 12px; margin-top: 32px;
  justify-content: flex-end;
}

@media (max-width: 640px) {
  .profile-page { padding: 20px 16px; }
  .profile-card { padding: 24px 20px; }
  .profile-card::before { left: 20px; right: 20px; }
  .profile-hero { flex-direction: column; text-align: center; }
  .profile-actions { flex-direction: column; }
}

/* Skeleton */
.skeleton-page-title {
  width: 120px;
  height: 28px;
  margin-bottom: 24px;
}

.skeleton-hero-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.skeleton-info-grid {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.skeleton-info-row {
  display: flex;
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px solid var(--border-subtle);
}

.skeleton-label {
  width: 80px;
  height: 14px;
  margin-right: 16px;
}

.skeleton-actions {
  display: flex;
  gap: 12px;
  margin-top: 28px;
}

.skeleton-btn {
  width: 100px;
  height: 36px;
  border-radius: 10px;
}

.skeleton-btn--wide {
  width: 120px;
}
</style>
