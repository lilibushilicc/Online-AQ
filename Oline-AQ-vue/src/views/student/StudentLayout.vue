<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft, Bell, EditPen, List, Trophy, Notebook, Reading,
  Menu, User, Setting, Help, SwitchButton,
} from '@element-plus/icons-vue'
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
const isDark = ref(document.documentElement.getAttribute('data-theme') !== 'light')

function toggleTheme() {
  isDark.value = !isDark.value
  document.documentElement.setAttribute('data-theme', isDark.value ? 'dark' : 'light')
  localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
}

const unreadCount = ref(0)
const announcements = ref<{ announcementId: number; title: string; content: string; createTime: string; read: boolean }[]>([])
const announceDialogVisible = ref(false)
const loginDialogVisible = ref(false)
const mobileMenuOpen = ref(false)

const navItems = [
  { path: '/student/exams', icon: Reading, label: '考试列表' },
  { path: '/student/results', icon: Trophy, label: '我的成绩' },
  { path: '/student/practice', icon: EditPen, label: '在线做题' },
  { path: '/student/practice-history', icon: List, label: '练习历史' },
  { path: '/student/wrong-book', icon: Notebook, label: '错题本' },
  { path: '/student/profile', icon: User, label: '个人中心' },
]

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

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/student/exams')
  }
}

function handleLogout() {
  store.logout()
  router.push('/login')
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
  <main class="page page--student">
    <CanvasAmbient />
    <div v-if="mobileMenuOpen" class="sidebar-backdrop" @click="mobileMenuOpen = false"></div>

    <!-- Sidebar -->
    <aside class="sidebar" :class="{ 'menu-open': mobileMenuOpen }">
      <div class="sidebar-brand">
        <div class="sidebar-brand-icon">S</div>
        <div class="sidebar-brand-text">
          <h2>在线答题</h2>
          <p>Student Portal</p>
        </div>
        <button class="sidebar-mobile-close" @click="mobileMenuOpen = false">
          <el-icon><ArrowLeft /></el-icon>
        </button>
      </div>

      <nav class="sidebar-nav">
        <a
          v-for="item in navItems"
          :key="item.path"
          :class="['sidebar-nav-item', { 'sidebar-nav-item--active': route.path.startsWith(item.path) }]"
          @click="router.push(item.path)"
        >
          <el-icon class="sidebar-nav-icon"><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </a>
      </nav>

      <div class="sidebar-bottom">
        <button class="sidebar-quiz-btn" @click="router.push('/student/practice')">
          <el-icon class="sidebar-quiz-icon"><EditPen /></el-icon>
          <span>快速练习</span>
        </button>
        <div class="sidebar-bottom-links">
          <a class="sidebar-bottom-link" @click.prevent>
            <el-icon><Setting /></el-icon>
            <span>设置</span>
          </a>
          <a class="sidebar-bottom-link" @click.prevent>
            <el-icon><Help /></el-icon>
            <span>帮助</span>
          </a>
        </div>
      </div>
    </aside>

    <!-- Top Header Bar -->
    <header class="topbar">
      <div class="topbar-left">
        <button class="topbar-hamburger" @click="mobileMenuOpen = true">
          <el-icon :size="20"><Menu /></el-icon>
        </button>
        <h1 class="topbar-title">{{ title }}</h1>
      </div>
      <div class="topbar-right">
        <button class="topbar-icon-btn" :title="isDark ? '切换为亮色' : '切换为暗色'" @click="toggleTheme">
          <el-icon :size="18">
            <svg v-if="isDark" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
            </svg>
            <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
            </svg>
          </el-icon>
        </button>
        <button class="topbar-icon-btn" title="通知" @click="openAnnounceDialog">
          <el-icon :size="20"><Bell /></el-icon>
          <span v-if="unreadCount > 0" class="topbar-badge"></span>
        </button>
        <div class="topbar-divider"></div>
        <div class="topbar-user" @click="router.push('/student/profile')">
          <div class="topbar-avatar">{{ userName.charAt(0) }}</div>
          <div class="topbar-user-text">
            <p class="topbar-user-name">{{ userName }}</p>
            <p class="topbar-user-role">Student</p>
          </div>
        </div>
        <button class="topbar-icon-btn topbar-icon-btn--logout" title="退出登录" @click="handleLogout">
          <el-icon :size="18"><SwitchButton /></el-icon>
        </button>
      </div>
    </header>

    <!-- Content -->
    <section class="content">
      <div v-if="showBack" class="back-nav">
        <el-button text :icon="ArrowLeft" @click="goBack">返回</el-button>
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
  </main>
</template>

<style scoped>
.page--student {
  display: flex;
  min-height: 100dvh;
  background: var(--bg-base);
  position: relative;
}

.sidebar-backdrop {
  display: none;
}

/* ===== Sidebar ===== */
.sidebar {
  position: fixed;
  top: 0;
  left: 0;
  width: 256px;
  height: 100vh;
  background: var(--bg-surface);
  border-right: 1px solid var(--border-subtle);
  display: flex;
  flex-direction: column;
  z-index: 100;
  padding: 24px 16px;
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  padding: 0 8px 20px;
  border-bottom: 1px solid var(--border-subtle);
}
.sidebar-brand-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-sm);
  background: var(--gradient-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  flex-shrink: 0;
}
.sidebar-brand-text h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
}
.sidebar-brand-text p {
  margin: 2px 0 0;
  font-size: 10px;
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-weight: 600;
}
.sidebar-mobile-close {
  display: none;
  margin-left: auto;
  background: none;
  border: none;
  color: var(--text-tertiary);
  cursor: pointer;
  padding: 4px;
}

.sidebar-nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 16px 0;
  overflow-y: auto;
}
.sidebar-nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
  font-size: 13px;
  font-weight: 500;
  text-decoration: none;
}
.sidebar-nav-item:hover {
  background: var(--sidebar-hover);
  color: var(--text-primary);
}
.sidebar-nav-item--active {
  background: rgba(87,241,219,0.1);
  color: var(--accent-primary);
  font-weight: 600;
}
.sidebar-nav-icon {
  font-size: 18px;
  flex-shrink: 0;
}

.sidebar-bottom {
  border-top: 1px solid var(--border-subtle);
  padding-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.sidebar-quiz-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 11px 16px;
  border: none;
  border-radius: var(--radius-sm);
  background: var(--gradient-primary);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(87,241,219,0.2);
  transition: all var(--duration-fast) var(--ease-out);
  font-family: var(--font-sans);
}
.sidebar-quiz-btn:hover {
  box-shadow: 0 6px 24px rgba(87,241,219,0.3);
  transform: translateY(-1px);
}
.sidebar-quiz-btn:active {
  transform: scale(0.97);
}
.sidebar-quiz-icon {
  font-size: 16px;
}
.sidebar-bottom-links {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.sidebar-bottom-link {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  color: var(--text-tertiary);
  cursor: pointer;
  transition: color var(--duration-fast);
  font-size: 13px;
  text-decoration: none;
}
.sidebar-bottom-link:hover {
  color: var(--accent-primary);
}
.sidebar-bottom-link .el-icon {
  font-size: 16px;
}

/* ===== Top Header Bar ===== */
.topbar {
  position: fixed;
  top: 0;
  left: 256px;
  right: 0;
  height: 60px;
  background: rgba(5,20,36,0.75);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid var(--border-subtle);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  z-index: 90;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.topbar-hamburger {
  display: none;
  background: none;
  border: none;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 4px;
}
.topbar-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.2px;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.topbar-icon-btn {
  position: relative;
  width: 34px;
  height: 34px;
  border-radius: var(--radius-sm);
  border: none;
  background: transparent;
  color: var(--text-tertiary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--duration-fast) var(--ease-out);
}
.topbar-icon-btn:hover {
  background: var(--sidebar-hover);
  color: var(--accent-primary);
}
.topbar-icon-btn--logout:hover {
  color: var(--accent-red);
}
.topbar-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--accent-red);
  border: 2px solid var(--bg-base);
}
.topbar-divider {
  width: 1px;
  height: 24px;
  background: var(--border-subtle);
}
.topbar-user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  transition: background var(--duration-fast);
}
.topbar-user:hover {
  background: var(--sidebar-hover);
}
.topbar-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--gradient-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
}
.topbar-user-text {
  text-align: right;
}
.topbar-user-name {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.2;
}
.topbar-user-role {
  margin: 1px 0 0;
  font-size: 10px;
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  font-weight: 600;
}

/* ===== Content ===== */
.content {
  margin-left: 256px;
  margin-top: 60px;
  flex: 1;
  padding: 28px 32px;
  min-height: calc(100vh - 60px);
  position: relative;
  z-index: 1;
}

.back-nav {
  margin-bottom: 16px;
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
  color: var(--accent-primary);
  background: var(--sidebar-hover);
}
.back-nav :deep(.el-button .el-icon) {
  margin-right: 2px;
  font-size: 16px;
}

/* ===== Announcements (keep existing) ===== */
.announce-item {
  padding: 12px 0;
}
.announce-item--unread {
  background: rgba(87,241,219,0.04);
  margin: 0 -16px;
  padding: 12px 16px;
  border-radius: var(--radius);
}
.announce-item__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.announce-item__header strong {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}
.announce-item__content {
  margin: 0;
  white-space: pre-wrap;
  line-height: 1.7;
  color: var(--text-secondary);
  font-size: 13px;
}

/* ===== Responsive ===== */
@media (max-width: 980px) {
  .sidebar {
    transform: translateX(-100%);
    transition: transform 0.3s var(--ease-out);
    box-shadow: 4px 0 24px rgba(0,0,0,0.3);
  }
  .sidebar.menu-open {
    transform: translateX(0);
  }
  .sidebar-mobile-close {
    display: flex;
  }
  .sidebar-backdrop {
    display: block;
    position: fixed;
    inset: 0;
    background: rgba(0,0,0,0.4);
    backdrop-filter: blur(4px);
    z-index: 99;
  }
  .topbar {
    left: 0;
    padding: 0 16px;
  }
  .topbar-hamburger {
    display: flex;
  }
  .content {
    margin-left: 0;
    margin-top: 60px;
    padding: 20px 16px;
  }
  .topbar-user-text {
    display: none;
  }
}
</style>
