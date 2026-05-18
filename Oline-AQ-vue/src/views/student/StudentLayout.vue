<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { EditPen, List, Trophy } from '@element-plus/icons-vue'
import { useExamStore } from '@/stores/exam'

defineProps<{ title: string; subtitle?: string }>()

const route = useRoute()
const store = useExamStore()
const userName = computed(() => store.currentUser?.realName ?? '学生')
</script>

<template>
  <main class="page">
    <section class="shell">
      <aside class="sidebar">
        <div class="brand">
          <div class="brand-logo">
            <div class="brand-icon">S</div>
            <div>
              <h2>在线答题</h2>
              <p>学生端作答中心</p>
            </div>
          </div>
        </div>

        <div class="user-card">
          <div class="user-avatar">{{ userName.charAt(0) }}</div>
          <div>
            <div class="user-name">{{ userName }}</div>
            <div class="user-role">学生</div>
          </div>
        </div>

        <el-menu
          :default-active="route.path"
          router
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

        <div class="sidebar-footer">AQ System v1.0</div>
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