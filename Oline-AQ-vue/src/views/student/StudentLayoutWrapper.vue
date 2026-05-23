<script setup lang="ts">
import { computed, watch, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useMobile } from '@/composables/useMobile'
import StudentLayout from './StudentLayout.vue'
import StudentMobileLayout from './StudentMobileLayout.vue'

const route = useRoute()
const meta = computed(() => route.meta as { title?: string; subtitle?: string; showBack?: boolean })
const routeKey = ref(0)
const { isMobile } = useMobile()

watch(() => route.path, () => { routeKey.value++ }, { immediate: true })
</script>

<template>
  <StudentMobileLayout v-if="isMobile" :title="meta.title || ''" :subtitle="meta.subtitle" :show-back="meta.showBack">
    <router-view v-slot="{ Component }">
      <component :is="Component" :key="routeKey" />
    </router-view>
  </StudentMobileLayout>

  <StudentLayout v-else :title="meta.title || ''" :subtitle="meta.subtitle">
    <template #actions v-if="meta.showBack">
      <el-button @click="$router.back()">返回</el-button>
    </template>
    <router-view v-slot="{ Component }">
      <component :is="Component" :key="routeKey" />
    </router-view>
  </StudentLayout>
</template>
