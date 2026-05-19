<script setup lang="ts">
withDefaults(defineProps<{
  items: Array<{ title: string; value: number | string; suffix?: string }>
  columns?: 2 | 3 | 4
}>(), { columns: 4 })
</script>

<template>
  <el-row :gutter="14">
    <el-col
      v-for="item in items"
      :key="item.title"
      :xs="24"
      :sm="12"
      :lg="columns === 2 ? 12 : columns === 3 ? 8 : 6"
      style="margin-bottom: 14px"
    >
      <el-card shadow="hover">
        <el-statistic :title="item.title" :value="item.value">
          <template v-if="item.suffix" #suffix>
            <span style="font-size: 14px; color: var(--muted)">{{ item.suffix }}</span>
          </template>
        </el-statistic>
      </el-card>
    </el-col>
  </el-row>
</template>
