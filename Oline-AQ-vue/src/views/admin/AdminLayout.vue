<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { HomeFilled, Upload, Collection, Document, DataAnalysis, User, Setting, ChatDotRound, Tickets, Fold, Expand, Menu, MagicStick, Message } from '@element-plus/icons-vue'
import { useExamStore } from '@/stores/exam'

defineProps<{ title: string; subtitle?: string }>()

const route = useRoute()
const store = useExamStore()
const userName = computed(() => store.currentUser?.realName ?? '教师')
const todayLabel = computed(() => new Intl.DateTimeFormat('zh-CN', {
  month: '2-digit',
  day: '2-digit',
  weekday: 'short',
}).format(new Date()))
const COLLAPSE_KEY = 'admin-sidebar-collapsed'
const isCollapsed = ref(localStorage.getItem(COLLAPSE_KEY) === 'true')
const mobileMenuOpen = ref(false)

function toggleCollapse() {
  isCollapsed.value = !isCollapsed.value
  localStorage.setItem(COLLAPSE_KEY, String(isCollapsed.value))
}

watch(() => route.path, () => {
  mobileMenuOpen.value = false
})
</script>

<template>
  <main class="page page--admin" :class="{ 'sidebar-collapsed': isCollapsed }">
    <section class="shell">
      <transition name="fade">
        <div v-if="mobileMenuOpen" class="sidebar-backdrop" @click="mobileMenuOpen = false"></div>
      </transition>

      <aside class="sidebar" :class="{ 'menu-open': mobileMenuOpen }">
        <div class="sidebar-header">
          <div class="brand">
            <div class="brand-logo">
              <div class="brand-icon">A</div>
              <div v-show="!isCollapsed">
                <h2>在线答题</h2>
                <p>教师端工作台</p>
              </div>
            </div>
          </div>
          <div class="mobile-hamburger" @click="mobileMenuOpen = !mobileMenuOpen">
            <el-icon :size="20"><Menu /></el-icon>
          </div>
        </div>

        <div v-show="!isCollapsed" class="user-card">
          <div class="user-avatar">{{ userName.charAt(0) }}</div>
          <div>
            <div class="user-name">{{ userName }}</div>
            <div class="user-role">管理员</div>
          </div>
          <div class="user-pulse animate-pulse-ring"></div>
        </div>

        <el-menu
          :default-active="route.path"
          :collapse="isCollapsed"
          router
        >
          <el-menu-item index="/admin/dashboard">
            <el-icon><HomeFilled /></el-icon>
            <span>首页</span>
          </el-menu-item>

          <el-sub-menu index="exam-group">
            <template #title>
              <el-icon><Document /></el-icon>
              <span>考试与试卷</span>
            </template>
            <el-menu-item index="/admin/papers">
              <el-icon><Edit /></el-icon>
              <span>试卷管理</span>
            </el-menu-item>
            <el-menu-item index="/admin/exams">
              <el-icon><Tickets /></el-icon>
              <span>考试管理</span>
            </el-menu-item>
            <el-menu-item index="/admin/results">
              <el-icon><DataAnalysis /></el-icon>
              <span>成绩查看</span>
            </el-menu-item>
            <el-menu-item index="/admin/review">
              <el-icon><Edit /></el-icon>
              <span>评分管理</span>
            </el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="question-group">
            <template #title>
              <el-icon><Collection /></el-icon>
              <span>题库资源</span>
            </template>
            <el-menu-item index="/admin/questions">
              <el-icon><Collection /></el-icon>
              <span>题库管理</span>
            </el-menu-item>
            <el-menu-item index="/admin/upload">
              <el-icon><Upload /></el-icon>
              <span>上传试题</span>
            </el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="user-group">
            <template #title>
              <el-icon><User /></el-icon>
              <span>人员管理</span>
            </template>
            <el-menu-item index="/admin/students">
              <el-icon><User /></el-icon>
              <span>学生管理</span>
            </el-menu-item>
            <el-menu-item index="/admin/feedbacks">
              <el-icon><ChatDotRound /></el-icon>
              <span>反馈管理</span>
            </el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="config-group">
            <template #title>
              <el-icon><Setting /></el-icon>
              <span>系统设置</span>
            </template>
            <el-menu-item index="/admin/config/storage">
              <el-icon><Setting /></el-icon>
              <span>存储设置</span>
            </el-menu-item>
            <el-menu-item index="/admin/config/ai">
              <el-icon><MagicStick /></el-icon>
              <span>AI 解析</span>
            </el-menu-item>
            <el-menu-item index="/admin/config/login">
              <el-icon><User /></el-icon>
              <span>登录设置</span>
            </el-menu-item>
            <el-menu-item index="/admin/config/email">
              <el-icon><Message /></el-icon>
              <span>邮箱注册</span>
            </el-menu-item>
            <el-menu-item index="/admin/announcements">
              <el-icon><ChatDotRound /></el-icon>
              <span>公告管理</span>
            </el-menu-item>
          </el-sub-menu>

          <el-menu-item index="/admin/profile">
            <el-icon><User /></el-icon>
            <span>个人中心</span>
          </el-menu-item>
        </el-menu>

        <div class="collapse-toggle" @click="toggleCollapse">
          <el-icon><Fold v-if="!isCollapsed" /><Expand v-else /></el-icon>
        </div>

        <div v-show="!isCollapsed" class="sidebar-footer">AQ System v1.0</div>
      </aside>

      <section class="content">
        <div class="page-title">
          <div class="page-title__copy">
            <div class="page-title__meta">
              <span class="page-chip">教师工作台</span>
              <span class="page-chip page-chip--soft">{{ todayLabel }}</span>
            </div>
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
