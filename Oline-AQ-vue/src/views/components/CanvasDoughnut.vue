<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps<{
  segments: { label: string; value: number; color: string }[]
  size?: number
  innerRadius?: number
}>()

const canvasRef = ref<HTMLCanvasElement | null>(null)
let rafId = 0

function getSurfaceBg() {
  return getComputedStyle(document.documentElement).getPropertyValue('--bg-surface').trim() || '#ffffff'
}

function draw() {
  const cvs = canvasRef.value
  if (!cvs) return
  const dpr = window.devicePixelRatio || 1
  const size = props.size ?? 180
  cvs.width = size * dpr
  cvs.height = size * dpr
  const ctx = cvs.getContext('2d')!
  ctx.scale(dpr, dpr)

  const cx = size / 2, cy = size / 2
  const outerR = size / 2 - 6
  const innerR = props.innerRadius ?? outerR * 0.55
  const total = props.segments.reduce((s, v) => s + v.value, 0) || 1

  ctx.clearRect(0, 0, size, size)

  ctx.shadowBlur = 20
  ctx.shadowColor = 'rgba(0,0,0,0.06)'

  ctx.beginPath()
  ctx.arc(cx, cy, outerR + 2, 0, Math.PI * 2)
  ctx.fillStyle = getSurfaceBg()
  ctx.fill()

  ctx.shadowBlur = 0
  ctx.beginPath()
  ctx.arc(cx, cy, outerR + 1, 0, Math.PI * 2)
  ctx.strokeStyle = 'rgba(87,241,219,0.06)'
  ctx.lineWidth = 1
  ctx.stroke()

  let startAngle = -Math.PI / 2

  for (const seg of props.segments) {
    if (seg.value === 0) continue
    const slice = (seg.value / total) * Math.PI * 2
    const endAngle = startAngle + slice - 0.03

    ctx.shadowBlur = 8
    ctx.shadowColor = seg.color + '30'

    ctx.beginPath()
    ctx.arc(cx, cy, outerR, startAngle, endAngle)
    ctx.arc(cx, cy, innerR, endAngle, startAngle, true)
    ctx.closePath()
    ctx.fillStyle = seg.color
    ctx.fill()

    startAngle += slice
  }

  ctx.shadowBlur = 0
  ctx.beginPath()
  ctx.arc(cx, cy, innerR - 1, 0, Math.PI * 2)
  ctx.fillStyle = getSurfaceBg()
  ctx.fill()

  ctx.beginPath()
  ctx.arc(cx, cy, innerR - 1, 0, Math.PI * 2)
  ctx.strokeStyle = 'rgba(0,0,0,0.03)'
  ctx.lineWidth = 1
  ctx.stroke()
}

const observer = new MutationObserver(() => {
  cancelAnimationFrame(rafId)
  rafId = requestAnimationFrame(draw)
})

onMounted(() => {
  draw()
  observer.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] })
  const onResize = () => { cancelAnimationFrame(rafId); rafId = requestAnimationFrame(draw) }
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  cancelAnimationFrame(rafId)
  observer.disconnect()
})
</script>

<template>
  <canvas ref="canvasRef" class="doughnut-canvas" :style="{ width: (size ?? 180) + 'px', height: (size ?? 180) + 'px' }" />
</template>

<style scoped>
.doughnut-canvas {
  display: block;
  margin: 0 auto;
}
</style>
