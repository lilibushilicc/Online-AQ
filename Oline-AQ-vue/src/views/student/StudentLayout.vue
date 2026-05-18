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

        <div style="padding: 12px 14px 8px; margin: 0 8px;">
          <div style="display: flex; align-items: center; gap: 10px; padding: 8px 10px; background: var(--primary-light); border-radius: var(--radius);">
            <div style="width: 32px; height: 32px; border-radius: 50%; background: var(--primary-gradient); color: #fff; display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 700; flex-shrink: 0;">
              {{ userName.charAt(0) }}
            </div>
            <div style="min-width: 0">
              <div style="font-size: 13px; font-weight: 600; color: var(--ink);">{{ userName }}</div>
              <div style="font-size: 11px; color: var(--muted);">学生</div>
            </div>
          </div>
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

        <div style="margin-top: auto; padding: 12px 16px; border-top: 1px solid var(--line-light); margin: auto 14px 0; font-size: 11px; color: var(--muted); text-align: center;">
          AQ System v1.0
        </div>
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
