<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  (e: 'complete'): void
}>()

const router = useRouter()

const phase = ref<'enter' | 'split' | 'exit' | 'done'>('enter')
const leftText = ref('')
const rightText = ref('')
const showLogin = ref(false)

const BRAND_TEXT = 'Online-AQ'
let timer: ReturnType<typeof setTimeout> | null = null

function splitText() {
  const mid = Math.ceil(BRAND_TEXT.length / 2)
  leftText.value = BRAND_TEXT.slice(0, mid)
  rightText.value = BRAND_TEXT.slice(mid)
}

function startAnimation() {
  phase.value = 'enter'
  splitText()

  timer = setTimeout(() => {
    phase.value = 'split'

    timer = setTimeout(() => {
      phase.value = 'exit'

      timer = setTimeout(() => {
        phase.value = 'done'
        showLogin.value = true
        emit('complete')
      }, 800)
    }, 800)
  }, 1000)
}

onMounted(() => {
  if (props.visible) {
    startAnimation()
  }
})

function handleClick() {
  if (timer) clearTimeout(timer)
  if (phase.value === 'enter') {
    phase.value = 'split'
    setTimeout(() => {
      phase.value = 'exit'
      setTimeout(() => {
        phase.value = 'done'
        showLogin.value = true
        emit('complete')
      }, 800)
    }, 800)
  } else if (phase.value === 'split' || phase.value === 'exit') {
    phase.value = 'exit'
    setTimeout(() => {
      phase.value = 'done'
      showLogin.value = true
      emit('complete')
    }, 500)
  }
}

defineExpose({ startAnimation })
</script>

<template>
  <div v-if="phase !== 'done'" class="brand-splash" :class="phase" @click="handleClick">
    <div class="brand-splash__word">
      <span class="brand-splash__half brand-splash__half--left">{{ leftText }}</span>
      <span class="brand-splash__half brand-splash__half--right">{{ rightText }}</span>
    </div>
    <p class="brand-splash__hint">点击任意处跳过</p>
  </div>
</template>

<style scoped>
.brand-splash {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: var(--bg-base, #051424);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  user-select: none;
}

.brand-splash__word {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
}

.brand-splash__half {
  font-family: 'Hanken Grotesk', sans-serif;
  font-size: 48px;
  font-weight: 700;
  line-height: 56px;
  letter-spacing: -0.02em;
  color: var(--text-primary, #d4e4fa);
  text-shadow: 0 0 40px rgba(87, 241, 219, 0.25), 0 0 80px rgba(87, 241, 219, 0.1);
  white-space: nowrap;
}

.brand-splash__hint {
  position: absolute;
  bottom: 40px;
  font-size: 12px;
  color: var(--text-tertiary, #859490);
  letter-spacing: 0.12em;
  text-transform: uppercase;
  font-family: 'Hanken Grotesk', sans-serif;
  opacity: 0.6;
}

/* Enter phase: fade in + float up */
.brand-splash.enter .brand-splash__half--left,
.brand-splash.enter .brand-splash__half--right {
  opacity: 0;
  transform: translateY(24px);
  animation: splashEnter 1s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

.brand-splash.enter .brand-splash__hint {
  opacity: 0;
  animation: hintFadeIn 0.6s ease-out 0.4s forwards;
}

/* Split phase: text splits apart */
.brand-splash.split .brand-splash__half--left {
  animation: splitLeft 0.8s cubic-bezier(0.7, 0, 0.84, 0) forwards;
}

.brand-splash.split .brand-splash__half--right {
  animation: splitRight 0.8s cubic-bezier(0.7, 0, 0.84, 0) forwards;
}

.brand-splash.split .brand-splash__hint {
  opacity: 0;
  transition: opacity 0.3s ease-out;
}

/* Exit phase: fade out */
.brand-splash.exit .brand-splash__half--left,
.brand-splash.exit .brand-splash__half--right,
.brand-splash.exit .brand-splash__hint {
  opacity: 0;
  filter: blur(4px);
  transition: opacity 0.5s, filter 0.5s;
}

.brand-splash.exit .brand-splash__hint {
  transition-delay: 0.1s;
}

@keyframes splashEnter {
  0% {
    opacity: 0;
    transform: translateY(24px);
    filter: blur(2px);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
    filter: blur(0);
  }
}

@keyframes hintFadeIn {
  0% {
    opacity: 0;
    transform: translateY(8px);
  }
  100% {
    opacity: 0.6;
    transform: translateY(0);
  }
}

@keyframes splitLeft {
  0% {
    opacity: 1;
    transform: translateX(0);
    filter: blur(0);
  }
  100% {
    opacity: 0;
    transform: translateX(-120px);
    filter: blur(8px);
  }
}

@keyframes splitRight {
  0% {
    opacity: 1;
    transform: translateX(0);
    filter: blur(0);
  }
  100% {
    opacity: 0;
    transform: translateX(120px);
    filter: blur(8px);
  }
}

/* 移动端适配 */
@media (max-width: 768px) {
  .brand-splash__half {
    font-size: 32px;
    font-weight: 700;
    line-height: 40px;
    letter-spacing: -0.01em;
  }
  
  .brand-splash__hint {
    bottom: 32px;
    font-size: 11px;
  }
}
</style>
