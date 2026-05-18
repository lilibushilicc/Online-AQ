<script setup lang="ts">
import { useRoute } from 'vue-router'
import { EditPen, List, Trophy } from '@element-plus/icons-vue'

defineProps<{ title: string; subtitle?: string }>()

const route = useRoute()
</script>

<template>
  <main class="page">
    <section class="shell">
      <aside class="sidebar">
        <div class="brand">
          <h2>在线答题</h2>
          <p>学生端作答中心</p>
        </div>
        <el-menu
          :default-active="route.path"
          router
          style="border-right: none"
        >
          <el-menu-item index="/student/exams">
            <el-icon><List /></el-icon>
            <span>考试列表</span>
          </el-menu-item>
          <el-menu-item index="/student/practice">
            <el-icon><EditPen /></el-icon>
            <span>在线做题</span>
          </el-menu-item>
          <el-menu-item index="/student/results">
            <el-icon><Trophy /></el-icon>
            <span>我的成绩</span>
          </el-menu-item>
        </el-menu>
      </aside>

      <section class="content">
        <div class="page-title">
          <div>
            <h1>{{ title }}</h1>
            <p v-if="subtitle">{{ subtitle }}</p>
          </div>
          <slot name="actions" />
        </div>
        <slot />
      </section>
    </section>
  </main>
</template>
