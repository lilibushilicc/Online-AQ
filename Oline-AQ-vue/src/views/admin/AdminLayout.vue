<script setup lang="ts">
import { useRoute } from 'vue-router'
import { HomeFilled, Upload, Collection, Document, DataAnalysis, User } from '@element-plus/icons-vue'

defineProps<{ title: string; subtitle?: string }>()

const route = useRoute()
</script>

<template>
  <main class="page">
    <section class="shell">
      <aside class="sidebar">
        <div class="brand">
          <h2>在线答题</h2>
          <p>教师端工作台</p>
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
