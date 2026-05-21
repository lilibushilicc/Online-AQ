<script setup lang="ts">
withDefaults(defineProps<{
  items: Array<{ title: string; value: number | string; suffix?: string }>
  columns?: 2 | 3 | 4
}>(), { columns: 4 })
</script>

<template>
  <div class="stat-cards" :class="[`stat-cards--${columns}`]">
    <div v-for="item in items" :key="item.title" class="stat-card">
      <div class="stat-card__label">{{ item.title }}</div>
      <div class="stat-card__value">
        {{ item.value }}
        <span v-if="item.suffix" class="stat-card__suffix">{{ item.suffix }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.stat-cards {
  display: grid;
  gap: 14px;
}

.stat-cards--2 {
  grid-template-columns: repeat(2, 1fr);
}

.stat-cards--3 {
  grid-template-columns: repeat(3, 1fr);
}

.stat-cards--4 {
  grid-template-columns: repeat(4, 1fr);
}

.stat-card {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.84), rgba(247, 245, 240, 0.96));
  border: 1px solid rgba(215, 208, 197, 0.92);
  border-radius: 14px;
  padding: 18px 20px;
  box-shadow: var(--shadow-sm);
  transition: box-shadow var(--duration), border-color var(--duration);
}

.stat-card:hover {
  border-color: var(--paper-dark);
  box-shadow: var(--shadow-md);
}

.stat-card__label {
  font-size: 11px;
  font-weight: 600;
  color: var(--muted);
  text-transform: uppercase;
  letter-spacing: 0.6px;
  margin-bottom: 6px;
}

.stat-card__value {
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.5px;
  color: var(--ink);
  line-height: 1.1;
  font-family: var(--font-serif);
}

.stat-card__suffix {
  font-size: 14px;
  font-weight: 400;
  color: var(--muted);
  letter-spacing: 0;
  margin-left: 2px;
}

/* Dark mode */
:root(html.dark) .stat-card {
  background: linear-gradient(180deg, rgba(34, 32, 30, 0.96), rgba(22, 21, 20, 0.98));
  border-color: var(--line);
}

:root(html.dark) .stat-card:hover {
  border-color: var(--line);
}

@media (max-width: 980px) {
  .stat-cards--4 {
    grid-template-columns: repeat(2, 1fr);
  }
  .stat-cards--3 {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 560px) {
  .stat-cards--4,
  .stat-cards--3,
  .stat-cards--2 {
    grid-template-columns: 1fr;
  }
}
</style>
