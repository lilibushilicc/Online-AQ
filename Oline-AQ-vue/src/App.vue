<script setup lang="ts">
import { computed, ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useExamStore } from '@/stores/exam'

const router = useRouter()
const store = useExamStore()
const logoutLabel = computed(() => store.currentUser?.role === 'admin' ? '教师端' : '学生端')

const isDark = ref(false)

function applyTheme(dark: boolean) {
  isDark.value = dark
  document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light')
  localStorage.setItem('theme', dark ? 'dark' : 'light')
}

function toggleTheme() {
  applyTheme(!isDark.value)
}

onMounted(() => {
  const attr = document.documentElement.getAttribute('data-theme')
  isDark.value = attr === 'dark'
})

function logout() {
  store.logout()
  router.push('/login')
}
</script>

<template>
  <router-view v-slot="{ Component, route: r }">
    <component :key="r.matched[0]?.path || 'login'" :is="Component" />
  </router-view>

  <el-button class="theme-toggle" circle @click="toggleTheme" :title="isDark ? '切换浅色' : '切换深色'">
    <el-icon :size="16">
      <!-- Sun icon for light mode, Moon icon for dark mode -->
      <svg v-if="isDark" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
      </svg>
      <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
      </svg>
    </el-icon>
  </el-button>

  <transition name="fade">
    <el-button v-if="store.currentUser" class="logout-button" @click="logout">
      <span class="logout-role">{{ logoutLabel }}</span>
      <el-icon style="font-size: 14px"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg></el-icon>
      退出登录
    </el-button>
  </transition>
</template>
