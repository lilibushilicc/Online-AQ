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
  <router-view v-slot="{ Component }">
    <component :is="Component" />
  </router-view>

  <el-button v-if="store.currentUser" class="logout-button" type="danger" plain @click="logout">
    退出登录
  </el-button>
</template>
