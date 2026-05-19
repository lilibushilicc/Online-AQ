<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { EditPen, List, Trophy, Notebook, Reading, Fold, Expand, Menu } from '@element-plus/icons-vue'
import { useExamStore } from '@/stores/exam'

defineProps<{ title: string; subtitle?: string }>()

const route = useRoute()
const store = useExamStore()
const userName = computed(() => store.currentUser?.realName ?? '学生')
const COLLAPSE_KEY = 'student-sidebar-collapsed'
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
  <main class="page" :class="{ 'sidebar-collapsed': isCollapsed }">
    <section class="shell">
      <aside class="sidebar" :class="{ 'menu-open': mobileMenuOpen }">
        <div class="sidebar-header">
          <div class="brand">
            <div class="brand-logo">
              <div class="brand-icon">S</div>
              <div v-show="!isCollapsed">
                <h2>在线答题</h2>
                <p>学生端作答中心</p>
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
            <div class="user-role">学生</div>
          </div>
        </div>

        <el-menu
          :default-active="route.path"
          :collapse="isCollapsed"
          router
        >
          <el-sub-menu index="study-center">
            <template #title>
              <el-icon><Reading /></el-icon>
              <span>学习中心</span>
            </template>
            <el-menu-item index="/student/exams">
              <el-icon><List /></el-icon>
              <span>考试列表</span>
            </el-menu-item>
            <el-menu-item index="/student/results">
              <el-icon><Trophy /></el-icon>
              <span>我的成绩</span>
            </el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="practice-tools">
            <template #title>
              <el-icon><EditPen /></el-icon>
              <span>练习提升</span>
            </template>
            <el-menu-item index="/student/practice">
              <el-icon><EditPen /></el-icon>
              <span>在线做题</span>
            </el-menu-item>
            <el-menu-item index="/student/wrong-book">
              <el-icon><Notebook /></el-icon>
              <span>错题本</span>
            </el-menu-item>
          </el-sub-menu>
        </el-menu>

        <div class="collapse-toggle" @click="toggleCollapse">
          <el-icon><Fold v-if="!isCollapsed" /><Expand v-else /></el-icon>
        </div>

        <div v-show="!isCollapsed" class="sidebar-footer">AQ System v1.0</div>
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
