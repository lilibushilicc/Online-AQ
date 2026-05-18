<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { HomeFilled, Upload, Collection, Document, DataAnalysis, User, Setting, ChatDotRound } from '@element-plus/icons-vue'
import { useExamStore } from '@/stores/exam'

defineProps<{ title: string; subtitle?: string }>()

const route = useRoute()
const store = useExamStore()
const userName = computed(() => store.currentUser?.realName ?? '教师')
</script>

<template>
  <main class="page">
    <section class="shell">
      <aside class="sidebar">
        <div class="brand">
          <div class="brand-logo">
            <div class="brand-icon">A</div>
            <div>
              <h2>在线答题</h2>
              <p>教师端工作台</p>
            </div>
          </div>
        </div>

        <div class="user-card">
          <div class="user-avatar">{{ userName.charAt(0) }}</div>
          <div>
            <div class="user-name">{{ userName }}</div>
            <div class="user-role">管理员</div>
          </div>
        </div>

        <el-menu
          :default-active="route.path"
          router
        >
          <el-menu-item index="/admin/dashboard">
            <el-icon><HomeFilled /></el-icon>
            <span>首页</span>
          </el-menu-item>
          <el-menu-item index="/admin/upload">
            <el-icon><Upload /></el-icon>
            <span>上传试题</span>
          </el-menu-item>
          <el-menu-item index="/admin/questions">
            <el-icon><Collection /></el-icon>
            <span>题库管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/exams">
            <el-icon><Document /></el-icon>
            <span>考试管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/students">
            <el-icon><User /></el-icon>
            <span>学生管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/results">
            <el-icon><DataAnalysis /></el-icon>
            <span>成绩查看</span>
          </el-menu-item>
          <el-menu-item index="/admin/feedbacks">
            <el-icon><ChatDotRound /></el-icon>
            <span>反馈管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/config">
            <el-icon><Setting /></el-icon>
            <span>系统设置</span>
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