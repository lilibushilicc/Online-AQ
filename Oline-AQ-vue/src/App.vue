<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useExamStore } from '@/stores/exam'

const router = useRouter()
const store = useExamStore()
const logoutLabel = computed(() => store.currentUser?.role === 'admin' ? '教师端' : '学生端')

function logout() {
  store.logout()
  router.push('/login')
}
</script>

<template>
  <router-view v-slot="{ Component, route: r }">
    <component :key="r.matched[0]?.path || 'login'" :is="Component" />
  </router-view>

  <transition name="fade">
    <el-button v-if="store.currentUser" class="logout-button" @click="logout">
      <span class="logout-role">{{ logoutLabel }}</span>
      <el-icon style="font-size: 14px"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg></el-icon>
      退出登录
    </el-button>
  </transition>
</template>
