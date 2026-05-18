<script setup lang="ts">
defineProps<{ title: string; subtitle?: string }>()
</script>

<template>
  <main class="page">
    <section class="shell">
      <aside class="sidebar">
        <div class="brand">
          <h2>在线答题</h2>
          <p>教师端工作台</p>
        </div>
        <nav class="nav">
          <router-link to="/admin/dashboard">首页</router-link>
          <router-link to="/admin/upload">上传试题</router-link>
          <router-link to="/admin/questions">题库管理</router-link>
          <router-link to="/admin/exams">考试管理</router-link>
          <router-link to="/admin/results">成绩查看</router-link>
        </nav>
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
