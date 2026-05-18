<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { HomeFilled, Upload, Collection, Document, DataAnalysis, User, Setting } from '@element-plus/icons-vue'
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

        <div style="padding: 12px 14px 8px; margin: 0 8px;">
          <div style="display: flex; align-items: center; gap: 10px; padding: 8px 10px; background: var(--primary-light); border-radius: var(--radius);">
            <div style="width: 32px; height: 32px; border-radius: 50%; background: var(--primary-gradient); color: #fff; display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 700; flex-shrink: 0;">
              {{ userName.charAt(0) }}
            </div>
            <div style="min-width: 0">
              <div style="font-size: 13px; font-weight: 600; color: var(--ink);">{{ userName }}</div>
              <div style="font-size: 11px; color: var(--muted);">管理员</div>
            </div>
          </div>
        </div>

        <el-menu
          :default-active="route.path"
          router
          style="border-right: none"
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
          <el-menu-item index="/admin/config">
            <el-icon><Setting /></el-icon>
            <span>系统设置</span>
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
