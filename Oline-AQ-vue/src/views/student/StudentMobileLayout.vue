<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as api from '@/api'
import { useExamStore } from '@/stores/exam'

const props = defineProps<{ title: string; subtitle?: string; showBack?: boolean }>()

const route = useRoute()
const router = useRouter()
const store = useExamStore()

const unreadCount = ref(0)
const announcements = ref<{ announcementId: number; title: string; content: string; createTime: string; read: boolean }[]>([])
const announceVisible = ref(false)

const tabs = [
  { label: '考试', path: '/student/exams', icon: 'orders-o' },
  { label: '成绩', path: '/student/results', icon: 'chart-trending-o' },
  { label: '练习', path: '/student/practice', icon: 'edit' },
  { label: '错题', path: '/student/wrong-book', icon: 'notes-o' },
  { label: '我的', path: '/student/profile', icon: 'manager-o' },
]

const activeTab = computed(() => {
  const matched = tabs.find((item) => route.path.startsWith(item.path))
  return matched?.path ?? '/student/exams'
})

const showBack = computed(() => {
  if (props.showBack) return true
  return /^\/student\/(exams\/\d+|results\/\d+|wrong-book\/[^/]+)$/.test(route.path)
})

const userName = computed(() => store.currentUser?.realName ?? '学生')

async function loadUnread() {
  try {
    const info = await api.getUnreadAnnouncementsApi()
    unreadCount.value = info.unreadCount
    announcements.value = info.announcements
  } catch {
    unreadCount.value = 0
  }
}

async function markOneRead(id: number) {
  try {
    await api.markAnnouncementReadApi(id)
    const target = announcements.value.find((item) => item.announcementId === id)
    if (target) target.read = true
    unreadCount.value = announcements.value.filter((item) => !item.read).length
  } catch {
    // ignore
  }
}

async function markAllRead() {
  try {
    await api.markAllAnnouncementsReadApi()
    unreadCount.value = 0
    announcements.value.forEach((item) => { item.read = true })
  } catch {
    // ignore
  }
}

function goBack() {
  router.back()
}

function switchTab(path: string) {
  if (path !== route.path) router.push(path)
}

watch(() => route.path, () => {
  announceVisible.value = false
})

onMounted(() => {
  void loadUnread()
})
</script>

<template>
  <div class="student-mobile-shell">
    <van-nav-bar fixed safe-area-inset-top :title="title || '学生端'" :left-arrow="showBack" @click-left="goBack">
      <template #right>
        <van-badge :content="unreadCount" :show-zero="false">
          <van-button size="small" plain hairline type="primary" class="student-mobile-shell__notice-btn" @click="announceVisible = true">
            公告
          </van-button>
        </van-badge>
      </template>
    </van-nav-bar>

    <main class="student-mobile-shell__body">
      <section class="student-mobile-shell__hero">
        <div class="student-mobile-shell__hero-main">
          <p class="student-mobile-shell__eyebrow">移动学习中心</p>
          <h1>{{ title || '学生端' }}</h1>
          <p v-if="subtitle" class="student-mobile-shell__subtitle">{{ subtitle }}</p>
        </div>
        <div class="student-mobile-shell__hero-side">
          <span class="student-mobile-shell__avatar">{{ userName.charAt(0) }}</span>
          <div>
            <div class="student-mobile-shell__name">{{ userName }}</div>
            <div class="student-mobile-shell__role">学生</div>
          </div>
        </div>
      </section>

      <section class="student-mobile-shell__content">
        <slot />
      </section>
    </main>

    <van-tabbar
      :model-value="activeTab"
      :route="false"
      fixed
      safe-area-inset-bottom
      active-color="var(--accent-blue)"
      inactive-color="var(--text-tertiary)"
      @update:model-value="switchTab"
    >
      <van-tabbar-item v-for="item in tabs" :key="item.path" :name="item.path" :icon="item.icon">
        {{ item.label }}
      </van-tabbar-item>
    </van-tabbar>

    <van-popup v-model:show="announceVisible" round position="bottom" class="student-mobile-shell__popup" @closed="markAllRead">
      <div class="student-mobile-shell__popup-head">
        <strong>系统公告</strong>
        <van-button size="small" plain hairline @click="markAllRead">全部已读</van-button>
      </div>
      <div v-if="announcements.length === 0" class="student-mobile-shell__empty">暂无公告</div>
      <div v-for="item in announcements" :key="item.announcementId" class="student-mobile-shell__announce-item">
        <div class="student-mobile-shell__announce-title-row">
          <strong>{{ item.title }}</strong>
          <van-tag v-if="!item.read" plain type="danger">未读</van-tag>
        </div>
        <div class="student-mobile-shell__announce-time">{{ item.createTime }}</div>
        <p class="student-mobile-shell__announce-content">{{ item.content }}</p>
        <van-button v-if="!item.read" size="small" plain hairline type="primary" @click="markOneRead(item.announcementId)">
          标记已读
        </van-button>
      </div>
    </van-popup>
  </div>
</template>
