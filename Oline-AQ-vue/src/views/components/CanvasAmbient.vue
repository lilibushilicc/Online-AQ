<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

interface Orb {
  x: number; y: number; r: number
  dx: number; dy: number
  color: string
  alpha: number
  pulsePhase: number
  pulseSpeed: number
}

const canvas = ref<HTMLCanvasElement>()
const orbs: Orb[] = []
const scale = ref(1)
let raf = 0
let w = 0, h = 0

const LIGHT_COLORS = [
  { c: '13,148,136', a: 0.08 },
  { c: '8,145,178', a: 0.06 },
  { c: '2,132,199', a: 0.05 },
  { c: '5,150,105', a: 0.06 },
  { c: '217,119,6', a: 0.04 },
]

const DARK_COLORS = [
  { c: '87,241,219', a: 0.06 },
  { c: '60,221,199', a: 0.05 },
  { c: '45,212,191', a: 0.04 },
  { c: '52,211,153', a: 0.03 },
  { c: '251,191,36', a: 0.02 },
]

function getColors() {
  const isDark = document.documentElement.getAttribute('data-theme') === 'dark'
  return isDark ? DARK_COLORS : LIGHT_COLORS
}

function initOrbs() {
  const colors = getColors()
  orbs.length = 0
  for (let i = 0; i < 5; i++) {
    const c = colors[i % colors.length]!
    orbs.push({
      x: Math.random() * w,
      y: Math.random() * h,
      r: 120 + Math.random() * 180,
      dx: (Math.random() - 0.5) * 0.2,
      dy: (Math.random() - 0.5) * 0.2,
      color: c.c,
      alpha: c.a,
      pulsePhase: Math.random() * Math.PI * 2,
      pulseSpeed: 0.003 + Math.random() * 0.005,
    })
  }
}

function draw() {
  const ctx = canvas.value?.getContext('2d')
  if (!ctx) return
  const s = scale.value
  ctx.setTransform(s, 0, 0, s, 0, 0)
  ctx.clearRect(0, 0, w, h)
  for (const o of orbs) {
    o.x += o.dx
    o.y += o.dy
    o.pulsePhase += o.pulseSpeed
    if (o.x < -o.r) o.x = w + o.r
    if (o.x > w + o.r) o.x = -o.r
    if (o.y < -o.r) o.y = h + o.r
    if (o.y > h + o.r) o.y = -o.r
    const pulse = 0.6 + 0.4 * Math.sin(o.pulsePhase)
    const grd = ctx.createRadialGradient(o.x, o.y, 0, o.x, o.y, o.r)
    grd.addColorStop(0, `rgba(${o.color},${o.alpha * pulse})`)
    grd.addColorStop(0.5, `rgba(${o.color},${o.alpha * pulse * 0.5})`)
    grd.addColorStop(1, `rgba(${o.color},0)`)
    ctx.fillStyle = grd
    ctx.fillRect(o.x - o.r, o.y - o.r, o.r * 2, o.r * 2)
  }
  ctx.setTransform(1, 0, 0, 1, 0, 0)
  raf = requestAnimationFrame(draw)
}

function resize() {
  w = window.innerWidth
  h = window.innerHeight
  if (canvas.value) {
    canvas.value.width = w * devicePixelRatio
    canvas.value.height = h * devicePixelRatio
    canvas.value.style.width = `${w}px`
    canvas.value.style.height = `${h}px`
    scale.value = devicePixelRatio
  }
  initOrbs()
}

let observer: MutationObserver | null = null

onMounted(() => {
  resize()
  window.addEventListener('resize', resize)
  raf = requestAnimationFrame(draw)
  observer = new MutationObserver(() => {
    initOrbs()
  })
  observer.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] })
})

onUnmounted(() => {
  cancelAnimationFrame(raf)
  window.removeEventListener('resize', resize)
  observer?.disconnect()
})
</script>

<template>
  <canvas ref="canvas" class="canvas-ambient" />
</template>

<style scoped>
.canvas-ambient {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}
</style>


