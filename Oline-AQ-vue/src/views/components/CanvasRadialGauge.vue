<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'

const props = withDefaults(defineProps<{
  value: number
  size?: number
  strokeWidth?: number
  color?: string
  trackColor?: string
  label?: string
  animated?: boolean
}>(), {
  size: 120,
  strokeWidth: 8,
  color: '#10b981',
  trackColor: '',
  label: '',
  animated: true,
})

const canvasRef = ref<HTMLCanvasElement | null>(null)
let rafId = 0
let currentVal = 0
let startTime = 0
let pulsePhase = 0
const duration = 1200

function getCSSVar(name: string, fallback: string) {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || fallback
}

function draw(value: number) {
  const cvs = canvasRef.value
  if (!cvs) return
  const dpr = window.devicePixelRatio || 1
  const size = props.size
  cvs.width = size * dpr
  cvs.height = size * dpr
  const ctx = cvs.getContext('2d')!
  ctx.scale(dpr, dpr)

  const cx = size / 2, cy = size / 2
  const r = size / 2 - props.strokeWidth - 4
  const arc = Math.min(value / 100, 1) * Math.PI * 2
  const pulse = Math.sin(pulsePhase) * 0.04 + 1

  ctx.clearRect(0, 0, size, size)

  ctx.shadowBlur = 0

  ctx.beginPath()
  ctx.arc(cx, cy, r + 2, 0, Math.PI * 2)
  ctx.strokeStyle = getCSSVar('--bg-overlay', '#e5e7eb')
  ctx.lineWidth = props.strokeWidth + 4
  ctx.lineCap = 'round'
  ctx.stroke()

  ctx.beginPath()
  ctx.arc(cx, cy, r, 0, Math.PI * 2)
  ctx.strokeStyle = getCSSVar('--border-subtle', '#eef0f4')
  ctx.lineWidth = props.strokeWidth
  ctx.lineCap = 'round'
  ctx.stroke()

  if (value > 0) {
    ctx.shadowBlur = 16 * pulse
    ctx.shadowColor = props.color + '60'

    const grad = ctx.createConicGradient(-Math.PI / 2, cx, cy)
    grad.addColorStop(0, props.color)
    grad.addColorStop(0.3, props.color + 'dd')
    grad.addColorStop(0.6, props.color + '99')
    grad.addColorStop(1, props.color + 'cc')

    ctx.beginPath()
    ctx.arc(cx, cy, r, -Math.PI / 2, -Math.PI / 2 + arc)
    ctx.strokeStyle = grad
    ctx.lineWidth = props.strokeWidth
    ctx.lineCap = 'round'
    ctx.stroke()

    ctx.shadowBlur = 0
  }

  const textColor = getCSSVar('--text-primary', '#1a1a2e')
  const mutedColor = getCSSVar('--text-tertiary', '#8c8ca1')

  ctx.fillStyle = textColor
  ctx.font = `bold ${size * 0.28}px "Inter", "Outfit", -apple-system, "PingFang SC", sans-serif`
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.shadowBlur = 0

  if (props.label) {
    ctx.fillText(`${Math.round(value)}%`, cx, cy - size * 0.05)
    ctx.fillStyle = mutedColor
    ctx.font = `500 ${size * 0.1}px "Inter", "Outfit", -apple-system, "PingFang SC", sans-serif`
    ctx.fillText(props.label, cx, cy + size * 0.17)
  } else {
    ctx.fillText(`${Math.round(value)}%`, cx, cy)
  }
}

function animate(timestamp: number) {
  if (!startTime) startTime = timestamp
  const elapsed = timestamp - startTime
  const progress = Math.min(elapsed / duration, 1)
  const ease = 1 - Math.pow(1 - progress, 3)
  currentVal = props.value * ease
  pulsePhase += 0.03
  draw(currentVal)
  if (progress < 1) {
    rafId = requestAnimationFrame(animate)
  } else {
    currentVal = props.value
    const pulseLoop = () => {
      pulsePhase += 0.02
      draw(currentVal)
      rafId = requestAnimationFrame(pulseLoop)
    }
    rafId = requestAnimationFrame(pulseLoop)
  }
}

function start() {
  startTime = 0
  cancelAnimationFrame(rafId)
  if (props.animated) {
    rafId = requestAnimationFrame(animate)
  } else {
    currentVal = props.value
    const loop = () => {
      pulsePhase += 0.02
      draw(currentVal)
      rafId = requestAnimationFrame(loop)
    }
    rafId = requestAnimationFrame(loop)
  }
}

const observer = new MutationObserver(start)

onMounted(() => {
  start()
  observer.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] })
  const onResize = () => start()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  cancelAnimationFrame(rafId)
  observer.disconnect()
})
</script>

<template>
  <canvas ref="canvasRef" class="radial-gauge" :style="{ width: size + 'px', height: size + 'px' }" />
</template>

<style scoped>
.radial-gauge {
  display: block;
}
</style>
