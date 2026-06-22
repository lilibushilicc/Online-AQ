<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  HomeFilled, Upload, Collection, Document, DataAnalysis, User, Setting,
  Tickets, ChatDotRound, MagicStick, Message, Menu, ArrowLeft, SwitchButton, Bell, Edit,
} from '@element-plus/icons-vue'
import { useExamStore } from '@/stores/exam'
import CanvasAmbient from '@/views/components/CanvasAmbient.vue'

defineProps<{ title: string; subtitle?: string }>()

const route = useRoute()
const router = useRouter()
const store = useExamStore()
const userName = computed(() => store.currentUser?.realName ?? '教师')
const todayLabel = computed(() => new Intl.DateTimeFormat('zh-CN', {
  month: '2-digit',
  day: '2-digit',
  weekday: 'short',
}).format(new Date()))
const isDark = ref(document.documentElement.getAttribute('data-theme') !== 'light')
const mobileMenuOpen = ref(false)

function toggleTheme() {
  isDark.value = !isDark.value
  document.documentElement.setAttribute('data-theme', isDark.value ? 'dark' : 'light')
  localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
}

function handleLogout() {
  store.logout()
  router.push('/login')
}

watch(() => route.path, () => {
  mobileMenuOpen.value = false
})

const navGroups = [
  {
    label: null,
    items: [
      { path: '/admin/dashboard', icon: HomeFilled, label: '首页' },
    ],
  },
  {
    label: '考试与试卷',
    items: [
      { path: '/admin/papers', icon: Document, label: '试卷管理' },
      { path: '/admin/exams', icon: Tickets, label: '考试管理' },
      { path: '/admin/results', icon: DataAnalysis, label: '成绩查看' },
      { path: '/admin/review', icon: Edit, label: '评分管理' },
    ],
  },
  {
    label: '题库资源',
    items: [
      { path: '/admin/questions', icon: Collection, label: '题库管理' },
      { path: '/admin/upload', icon: Upload, label: '上传试题' },
    ],
  },
  {
    label: '人员管理',
    items: [
      { path: '/admin/students', icon: User, label: '学生管理' },
      { path: '/admin/feedbacks', icon: ChatDotRound, label: '反馈管理' },
    ],
  },
  {
    label: '系统设置',
    items: [
      { path: '/admin/config/storage', icon: Setting, label: '存储设置' },
      { path: '/admin/config/ai', icon: MagicStick, label: 'AI 解析' },
      { path: '/admin/config/login', icon: User, label: '登录设置' },
      { path: '/admin/config/email', icon: Message, label: '邮箱注册' },
      { path: '/admin/config/version', icon: Setting, label: '版本更新' },
      { path: '/admin/announcements', icon: ChatDotRound, label: '公告管理' },
    ],
  },
  {
    label: null,
    items: [
      { path: '/admin/profile', icon: User, label: '个人中心' },
    ],
  },
]
</script>

<template>
  <main class="page page--admin">
    <CanvasAmbient />
    <div v-if="mobileMenuOpen" class="sidebar-backdrop" @click="mobileMenuOpen = false"></div>

    <!-- Sidebar -->
    <aside class="sidebar" :class="{ 'menu-open': mobileMenuOpen }">
      <div class="sidebar-brand">
        <div class="sidebar-brand-icon">A</div>
        <div class="sidebar-brand-text">
          <h2>在线答题</h2>
          <p>教师端工作台</p>
        </div>
        <button class="sidebar-mobile-close" @click="mobileMenuOpen = false">
          <el-icon><ArrowLeft /></el-icon>
        </button>
      </div>

      <nav class="sidebar-nav">
        <template v-for="group in navGroups" :key="group.label || group.items[0].path">
          <div v-if="group.label" class="sidebar-nav-group-label">{{ group.label }}</div>
          <a
            v-for="item in group.items"
            :key="item.path"
            :class="['sidebar-nav-item', { 'sidebar-nav-item--active': route.path.startsWith(item.path) }]"
            @click="router.push(item.path)"
          >
            <el-icon class="sidebar-nav-icon"><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </a>
        </template>
      </nav>

      <div class="sidebar-bottom">
        <div class="sidebar-footer-text">AQ System v1.0</div>
      </div>
    </aside>

    <!-- Top Header Bar -->
    <header class="topbar">
      <div class="topbar-left">
        <button class="topbar-hamburger" @click="mobileMenuOpen = true">
          <el-icon :size="20"><Menu /></el-icon>
        </button>
        <div class="topbar-meta">
          <span class="page-chip">教师工作台</span>
          <span class="page-chip page-chip--soft">{{ todayLabel }}</span>
        </div>
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
        <button class="topbar-icon-btn" title="通知">
          <el-icon :size="20"><Bell /></el-icon>
        </button>
        <div class="topbar-divider"></div>
        <div class="topbar-user" @click="router.push('/admin/profile')">
          <div class="topbar-avatar">{{ userName.charAt(0) }}</div>
          <div class="topbar-user-text">
            <p class="topbar-user-name">{{ userName }}</p>
            <p class="topbar-user-role">Admin</p>
          </div>
        </div>
        <button class="topbar-icon-btn topbar-icon-btn--logout" title="退出登录" @click="handleLogout">
          <el-icon :size="18"><SwitchButton /></el-icon>
        </button>
      </div>
    </header>

    <!-- Content -->
    <section class="content">
      <slot name="actions" />
      <slot />
    </section>
  </main>
</template>

<style scoped>
.page--admin {
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
  padding: 12px 0;
  overflow-y: auto;
}
.sidebar-nav-group-label {
  font-size: 10px;
  font-weight: 600;
  color: var(--text-disabled);
  text-transform: uppercase;
  letter-spacing: 0.12em;
  padding: 16px 12px 6px;
}
.sidebar-nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
  font-size: 13px;
  font-weight: 500;
  text-decoration: none;
  margin-left: 0;
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
  font-size: 16px;
  flex-shrink: 0;
  opacity: 0.7;
}
.sidebar-nav-item--active .sidebar-nav-icon {
  opacity: 1;
}

.sidebar-bottom {
  border-top: 1px solid var(--border-subtle);
  padding-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.sidebar-footer-text {
  text-align: center;
  font-size: 11px;
  color: var(--text-disabled);
  letter-spacing: 0.3px;
  padding: 4px 0;
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
  min-width: 0;
}
.topbar-hamburger {
  display: none;
  background: none;
  border: none;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 4px;
}
.topbar-meta {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-shrink: 0;
}
.topbar-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
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
  .topbar-meta {
    display: none;
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
