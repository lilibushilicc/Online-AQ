<script setup lang="ts">
import { computed, ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useExamStore } from '@/stores/exam'
import BrandSplash from '@/views/components/BrandSplash.vue'

const router = useRouter()
const route = useRoute()
const store = useExamStore()
const logoutLabel = computed(() => store.currentUser?.role === 'admin' ? '教师端' : '学生端')
const isLayoutPage = computed(() => route.path.startsWith('/student') || route.path.startsWith('/admin'))

const isDark = ref(false)
const showSplash = ref(false)
const splashComplete = ref(false)

function applyTheme(dark: boolean) {
  isDark.value = dark
  document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light')
  localStorage.setItem('theme', dark ? 'dark' : 'light')
}

function toggleTheme() {
  applyTheme(!isDark.value)
}

function onSplashComplete() {
  splashComplete.value = true
  const hasSeen = localStorage.getItem('splash-seen')
  if (!hasSeen) {
    localStorage.setItem('splash-seen', 'true')
  }
  setTimeout(() => {
    router.push('/login')
  }, 100)
}

function shouldShowSplash() {
  if (store.currentUser) return false
  if (route.path !== '/') return false
  return !localStorage.getItem('splash-seen')
}

watch(() => route.fullPath, () => {
  showSplash.value = shouldShowSplash()
  splashComplete.value = false
}, { immediate: true })

onMounted(() => {
  const attr = document.documentElement.getAttribute('data-theme')
  isDark.value = true; document.documentElement.setAttribute('data-theme', 'dark')
  showSplash.value = shouldShowSplash()
})

function logout() {
  store.logout()
  router.push('/login')
}
</script>

<template>
  <div v-if="showSplash && !splashComplete" class="splash-wrapper">
    <BrandSplash :visible="showSplash" @complete="onSplashComplete" />
  </div>
  <router-view v-else />


  <el-button v-if="!isLayoutPage" class="theme-toggle" circle @click="toggleTheme" :title="isDark ? '切换为亮色' : '切换为暗色'">
    <el-icon :size="16">
      <svg v-if="isDark" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
      </svg>
      <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
      </svg>
    </el-icon>
  </el-button>

  <transition name="fade">
    <el-button v-if="store.currentUser && !isLayoutPage" class="logout-button" @click="logout">
      <span class="logout-role">{{ logoutLabel }}</span>
      <span class="logout-label">退出登录</span>
      <span class="logout-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
      </span>
    </el-button>
  </transition>
</template>

