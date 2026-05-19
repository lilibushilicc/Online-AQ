<script setup lang="ts">
import { computed, watch, ref } from 'vue'
import { useRoute } from 'vue-router'
import StudentLayout from './StudentLayout.vue'

const route = useRoute()
const meta = computed(() => route.meta as { title?: string; subtitle?: string; showBack?: boolean })
const routeKey = ref(0)

watch(() => route.path, () => { routeKey.value++ }, { immediate: true })
</script>

<template>
  <StudentLayout :title="meta.title || ''" :subtitle="meta.subtitle">
    <template #actions v-if="meta.showBack">
      <el-button @click="$router.back()">返回</el-button>
    </template>
    <router-view v-slot="{ Component }">
      <component :is="Component" :key="routeKey" />
    </router-view>
  </StudentLayout>
</template>
