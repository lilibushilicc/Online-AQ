<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Bell, EditPen, List, Trophy, Notebook, Reading, Fold, Expand, Menu, User } from '@element-plus/icons-vue'
import { useExamStore } from '@/stores/exam'
import * as api from '@/api'
import { useNotification } from '@/composables/useNotification'
import CanvasAmbient from '@/views/components/CanvasAmbient.vue'

defineProps<{ title: string; subtitle?: string; showBack?: boolean }>()

const route = useRoute()
const router = useRouter()
const store = useExamStore()
const userName = computed(() => store.currentUser?.realName ?? '学生')
const todayLabel = computed(() => new Intl.DateTimeFormat('zh-CN', {
  month: '2-digit',
  day: '2-digit',
  weekday: 'short',
}).format(new Date()))
const COLLAPSE_KEY = 'student-sidebar-collapsed'
const isCollapsed = ref(localStorage.getItem(COLLAPSE_KEY) === 'true')
const mobileMenuOpen = ref(false)

const unreadCount = ref(0)
const announcements = ref<{ announcementId: number; title: string; content: string; createTime: string; read: boolean }[]>([])
const announceDialogVisible = ref(false)
const loginDialogVisible = ref(false)

async function loadUnread() {
  try {
    const info = await api.getUnreadAnnouncementsApi()
    unreadCount.value = info.unreadCount
    announcements.value = info.announcements
  } catch { /* ignore */ }
}

function openAnnounceDialog() {
  announceDialogVisible.value = true
}

async function markAllRead() {
  try {
    await api.markAllAnnouncementsReadApi()
    unreadCount.value = 0
    announcements.value.forEach(a => { a.read = true })
  } catch { /* ignore */ }
}

async function markOneRead(id: number) {
  try {
    await api.markAnnouncementReadApi(id)
    const item = announcements.value.find(a => a.announcementId === id)
    if (item) item.read = true
    unreadCount.value = announcements.value.filter(a => !a.read).length
  } catch { /* ignore */ }
}

function closeLoginDialog() {
  loginDialogVisible.value = false
  markAllRead()
}

function toggleCollapse() {
  isCollapsed.value = !isCollapsed.value
  localStorage.setItem(COLLAPSE_KEY, String(isCollapsed.value))
}

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/student/dashboard')
  }
}

watch(() => route.path, () => {
  mobileMenuOpen.value = false
})

const { connect: connectSSE } = useNotification()

onMounted(async () => {
  await loadUnread()
  if (unreadCount.value > 0) {
    loginDialogVisible.value = true
  }
  connectSSE()
})
</script>

<template>
  <main class="page page--student" :class="{ 'sidebar-collapsed': isCollapsed }">
    <CanvasAmbient />
    <section class="shell">
      <transition name="fade">
        <div v-if="mobileMenuOpen" class="sidebar-backdrop" @click="mobileMenuOpen = false"></div>
      </transition>

      <aside class="sidebar" :class="{ 'menu-open': mobileMenuOpen }">
        <div class="sidebar-header">
          <div class="brand">
            <div class="brand-logo">
              <div class="brand-icon">S</div>
              <div v-show="!isCollapsed">
                <h2>在线答题</h2>
                <p>学生端作答中心</p>
              </div>
            </div>
          </div>
          <div class="mobile-hamburger" @click="mobileMenuOpen = !mobileMenuOpen">
            <el-icon :size="20"><Menu /></el-icon>
          </div>
        </div>

        <div v-show="!isCollapsed" class="user-card">
          <div class="user-avatar">{{ userName.charAt(0) }}</div>
          <div>
            <div class="user-name">{{ userName }}</div>
            <div class="user-role">学生</div>
          </div>
          <div class="user-pulse animate-pulse-ring"></div>
        </div>

        <el-menu
          :default-active="route.path"
          :collapse="isCollapsed"
          router
        >
          <el-sub-menu index="study-center">
            <template #title>
              <el-icon><Reading /></el-icon>
              <span>学习中心</span>
            </template>
            <el-menu-item index="/student/exams">
              <el-icon><List /></el-icon>
              <span>考试列表</span>
            </el-menu-item>
            <el-menu-item index="/student/results">
              <el-icon><Trophy /></el-icon>
              <span>我的成绩</span>
            </el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="practice-tools">
            <template #title>
              <el-icon><EditPen /></el-icon>
              <span>练习提升</span>
            </template>
            <el-menu-item index="/student/practice">
              <el-icon><EditPen /></el-icon>
              <span>在线做题</span>
            </el-menu-item>
            <el-menu-item index="/student/practice-history">
              <el-icon><List /></el-icon>
              <span>练习历史</span>
            </el-menu-item>
            <el-menu-item index="/student/wrong-book">
              <el-icon><Notebook /></el-icon>
              <span>错题本</span>
            </el-menu-item>
          </el-sub-menu>

          <el-menu-item index="/student/profile">
            <el-icon><User /></el-icon>
            <span>个人中心</span>
          </el-menu-item>
        </el-menu>

        <div class="collapse-toggle" @click="toggleCollapse">
          <el-icon><Fold v-if="!isCollapsed" /><Expand v-else /></el-icon>
        </div>

        <div v-show="!isCollapsed" class="sidebar-footer">AQ System v1.0</div>
      </aside>

      <section class="content">
        <div v-if="showBack" class="back-nav">
          <el-button text :icon="ArrowLeft" @click="goBack">返回</el-button>
        </div>
        <div class="page-title">
          <div class="page-title__copy">
            <div class="page-title__meta">
              <span class="page-chip">学生作答中心</span>
              <span class="page-chip page-chip--soft">{{ todayLabel }}</span>
            </div>
            <h1>{{ title }}</h1>
            <p v-if="subtitle">{{ subtitle }}</p>
          </div>
          <div style="display: flex; align-items: center; gap: 8px">
            <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="announce-badge">
              <el-button size="small" :icon="Bell" circle @click="openAnnounceDialog" />
            </el-badge>
            <slot name="actions" />
          </div>
        </div>
        <slot />

        <!-- 公告列表弹窗 -->
        <el-dialog v-model="announceDialogVisible" title="系统公告" width="560px" @close="markAllRead">
          <div v-if="announcements.length === 0" style="text-align: center; padding: 40px 0; color: var(--text-tertiary)">暂无公告</div>
          <div v-for="ann in announcements" :key="ann.announcementId" class="announce-item" :class="{ 'announce-item--unread': !ann.read }">
            <div class="announce-item__header">
              <strong>{{ ann.title }}</strong>
              <div style="display: flex; align-items: center; gap: 6px">
                <span style="font-size: 12px; color: var(--text-tertiary)">{{ ann.createTime }}</span>
                <el-tag v-if="!ann.read" size="small" type="danger">未读</el-tag>
              </div>
            </div>
            <p class="announce-item__content">{{ ann.content }}</p>
            <div v-if="!ann.read" style="text-align: right">
              <el-button size="small" type="primary" plain @click="markOneRead(ann.announcementId)">标记已读</el-button>
            </div>
            <el-divider v-if="ann !== announcements[announcements.length - 1]" />
          </div>
        </el-dialog>

        <!-- 登录后未读公告弹窗 -->
        <el-dialog v-model="loginDialogVisible" title="📢 系统公告" width="520px" :close-on-click-modal="false" @close="closeLoginDialog">
          <div v-for="ann in announcements.filter(a => !a.read)" :key="ann.announcementId" style="margin-bottom: 18px">
            <strong style="font-size: 15px">{{ ann.title }}</strong>
            <p style="margin-top: 8px; white-space: pre-wrap; line-height: 1.7; color: var(--text-secondary)">{{ ann.content }}</p>
          </div>
          <template #footer>
            <el-button type="primary" @click="closeLoginDialog">我知道了</el-button>
          </template>
        </el-dialog>
      </section>
    </section>
  </main>
</template>

<style scoped>
.announce-badge :deep(.el-badge__content) {
  top: 6px;
  right: 6px;
}

.back-nav {
  margin-bottom: 8px;
}

.back-nav .el-button {
  font-size: 13px;
  color: var(--text-tertiary);
  padding: 4px 10px 4px 4px;
  height: auto;
  border-radius: var(--radius-sm);
  transition: color var(--duration-fast), background var(--duration-fast);
}

.back-nav .el-button:hover {
  color: var(--accent-blue);
  background: var(--sidebar-hover);
}

.back-nav :deep(.el-button .el-icon) {
  margin-right: 2px;
  font-size: 16px;
}
</style>
