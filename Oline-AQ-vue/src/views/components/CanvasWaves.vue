<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

const canvas = ref<HTMLCanvasElement>()
let raf = 0
let phase = 0

function getColor(alpha: number) {
  const isDark = document.documentElement.getAttribute('data-theme') === 'dark'
  const c = isDark ? '45,212,191' : '13,148,136'
  return `rgba(${c},${alpha})`
}

function draw() {
  const cvs = canvas.value
  if (!cvs) return
  const ctx = cvs.getContext('2d')
  if (!ctx) return
  const s = devicePixelRatio
  ctx.setTransform(s, 0, 0, s, 0, 0)
  const cw = cvs.width / s, ch = cvs.height / s
  ctx.clearRect(0, 0, cw, ch)
  phase += 0.015

  const drawWave = (offsetY: number, amp: number, freq: number, alpha: number, phaseOff: number) => {
    ctx.beginPath()
    ctx.moveTo(0, ch)
    for (let x = 0; x <= cw; x += 2) {
      const y = offsetY + Math.sin((x + phaseOff) * freq + phase) * amp + Math.sin((x * 0.3 + phaseOff) * freq * 0.7 + phase * 0.6) * amp * 0.4
      ctx.lineTo(x, y)
    }
    ctx.lineTo(cw, ch)
    ctx.closePath()
    ctx.fillStyle = getColor(alpha)
    ctx.fill()
  }

  drawWave(ch - 40, 14, 0.006, 0.06, 0)
  drawWave(ch - 30, 10, 0.008, 0.04, 2)
  drawWave(ch - 20, 8, 0.01, 0.03, 4)

  ctx.setTransform(1, 0, 0, 1, 0, 0)
  raf = requestAnimationFrame(draw)
}

function resize() {
  const cvs = canvas.value
  if (!cvs) return
  const parent = cvs.parentElement
  if (!parent) return
  cvs.width = parent.clientWidth * devicePixelRatio
  cvs.height = parent.clientHeight * devicePixelRatio
  cvs.style.width = `${parent.clientWidth}px`
  cvs.style.height = `${parent.clientHeight}px`
}

let observer: MutationObserver | null = null

onMounted(() => {
  resize()
  window.addEventListener('resize', resize)
  raf = requestAnimationFrame(draw)
  observer = new MutationObserver(() => {})
  observer.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] })
})

onUnmounted(() => {
  cancelAnimationFrame(raf)
  window.removeEventListener('resize', resize)
  observer?.disconnect()
})
</script>

<template>
  <canvas ref="canvas" class="canvas-waves" />
</template>

<style scoped>
.canvas-waves {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 80px;
  pointer-events: none;
  z-index: 1;
}
</style>
