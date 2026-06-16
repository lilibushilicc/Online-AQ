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
  background: var(--bg-surface);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-lg);
  padding: 18px 20px;
  box-shadow: var(--shadow-sm);
  transition: box-shadow var(--duration), border-color var(--duration);
}

.stat-card:hover {
  border-color: var(--accent-blue);
  border-color: color-mix(in srgb, var(--accent-blue) 20%, transparent);
  box-shadow: 0 0 20px var(--glow-blue);
}

.stat-card__label {
  font-size: var(--text-label);
  font-weight: 600;
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.6px;
  margin-bottom: 6px;
}

.stat-card__value {
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.5px;
  color: var(--text-primary);
  line-height: 1.1;
  font-variant-numeric: tabular-nums;
}

.stat-card__suffix {
  font-size: var(--text-body);
  font-weight: 400;
  color: var(--text-tertiary);
  letter-spacing: 0;
  margin-left: 2px;
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
