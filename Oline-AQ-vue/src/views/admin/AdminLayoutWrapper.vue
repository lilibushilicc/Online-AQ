<script setup lang="ts">
import { computed, watch, ref } from 'vue'
import { useRoute } from 'vue-router'
import AdminLayout from './AdminLayout.vue'

const route = useRoute()
const meta = computed(() => route.meta as { title?: string; subtitle?: string })
const routeKey = ref(0)

watch(() => route.path, () => { routeKey.value++ }, { immediate: true })
</script>

<template>
  <AdminLayout :title="meta.title || ''" :subtitle="meta.subtitle">
    <router-view v-slot="{ Component }">
      <component :is="Component" :key="routeKey" />
    </router-view>
  </AdminLayout>
</template>
