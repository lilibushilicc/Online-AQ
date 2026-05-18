<script setup lang="ts">
defineProps<{ title: string; subtitle?: string }>()
</script>

<template>
  <main class="page">
    <section class="shell">
      <aside class="sidebar">
        <div class="brand">
          <h2>在线答题</h2>
          <p>学生端作答中心</p>
        </div>
        <nav class="nav">
          <router-link to="/student/exams">考试列表</router-link>
          <router-link to="/student/results">我的成绩</router-link>
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
