<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useExamStore } from '@/stores/exam'

const router = useRouter()
const store = useExamStore()

function logout() {
  store.logout()
  router.push('/login')
}
</script>

<template>
  <router-view v-slot="{ Component, route: r }">
    <transition name="page" mode="out-in">
      <component :key="r.path.split('/')[1] || 'login'" :is="Component" />
    </transition>
  </router-view>

  <transition name="fade">
    <el-button v-if="store.currentUser" class="logout-button" type="danger" plain size="small" @click="logout">
      退出登录
    </el-button>
  </transition>
</template>
