<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'

const canvasRef = ref<HTMLCanvasElement | null>(null)

interface Particle {
  x: number; y: number
  vx: number; vy: number
  r: number; baseR: number
  alpha: number
  hue: number
  pulseSpeed: number
  pulsePhase: number
}

interface Ribbon {
  points: { x: number; y: number; alpha: number }[]
  hue: number
  speed: number
  phase: number
}

let particles: Particle[] = []
let ribbons: Ribbon[] = []
let w = 0, h = 0
let rafId = 0
let time = 0
const mouse = { x: -9999, y: -9999, prevX: -9999, prevY: -9999, speed: 0 }

function resize(cvs: HTMLCanvasElement) {
  w = cvs.width = window.innerWidth
  h = cvs.height = window.innerHeight
}

function init(cvs: HTMLCanvasElement) {
  resize(cvs)
  const count = Math.min(150, Math.floor(w * h / 12000))
  particles = Array.from({ length: count }, () => ({
    x: Math.random() * w,
    y: Math.random() * h,
    vx: (Math.random() - 0.5) * 0.8,
    vy: (Math.random() - 0.5) * 0.8,
    r: Math.random() * 6 + 2,
    baseR: 0,
    alpha: Math.random() * 0.5 + 0.3,
    hue: Math.random() < 0.6 ? 217 : Math.random() < 0.5 ? 270 : 190,
    pulseSpeed: Math.random() * 0.02 + 0.01,
    pulsePhase: Math.random() * Math.PI * 2,
  }))
  particles.forEach(p => { p.baseR = p.r })

  const hues = [217, 270, 190]
  ribbons = Array.from({ length: 3 }, (_, i) => ({
    points: Array.from({ length: 30 }, (_, j) => ({
      x: (j / 29) * w,
      y: h * (0.3 + i * 0.2) + Math.sin(j * 0.3) * 40,
      alpha: 0,
    })),
    hue: hues[i] ?? 217,
    speed: 0.3 + i * 0.15,
    phase: i * 2,
  }))
}

function draw(cvs: HTMLCanvasElement) {
  const ctx = cvs.getContext('2d')!
  time += 0.008

  ctx.clearRect(0, 0, w, h)

  ctx.shadowBlur = 0

  for (const rib of ribbons) {
    for (let i = 0; i < rib.points.length; i++) {
      const p = rib.points[i]!
      const t = time * rib.speed + i * 0.15 + rib.phase
      p.y += Math.sin(t + i * 0.1) * 0.4
      p.x += Math.cos(t * 0.7 + i * 0.05) * 0.3
      if (p.x < -20) p.x = w + 20
      if (p.x > w + 20) p.x = -20
      p.alpha = (1 - Math.abs(i / (rib.points.length - 1) - 0.5) * 1.6) * 0.15
    }

    const p0 = rib.points[0]!
    ctx.beginPath()
    ctx.moveTo(p0.x, p0.y)
    for (let i = 1; i < rib.points.length - 1; i++) {
      const pi = rib.points[i]!
      const pn = rib.points[i + 1]!
      const xc = (pi.x + pn.x) / 2
      const yc = (pi.y + pn.y) / 2
      ctx.quadraticCurveTo(pi.x, pi.y, xc, yc)
    }
    ctx.strokeStyle = `hsla(${rib.hue}, 80%, 65%, 0.12)`
    ctx.lineWidth = 3
    ctx.stroke()
  }

  ctx.shadowBlur = 0

  for (const p of particles) {
    p.r = p.baseR + Math.sin(time * p.pulseSpeed * 20 + p.pulsePhase) * 1.2
    p.x += p.vx; p.y += p.vy
    if (p.x < -20) p.x = w + 20
    if (p.x > w + 20) p.x = -20
    if (p.y < -20) p.y = h + 20
    if (p.y > h + 20) p.y = -20

    ctx.shadowBlur = p.r * 4
    ctx.shadowColor = `hsla(${p.hue}, 80%, 60%, ${p.alpha * 0.5})`

    const grad = ctx.createRadialGradient(p.x, p.y, 0, p.x, p.y, p.r * 2)
    grad.addColorStop(0, `hsla(${p.hue}, 80%, 75%, ${p.alpha})`)
    grad.addColorStop(0.4, `hsla(${p.hue}, 80%, 60%, ${p.alpha * 0.4})`)
    grad.addColorStop(1, `hsla(${p.hue}, 80%, 60%, 0)`)

    ctx.beginPath()
    ctx.arc(p.x, p.y, p.r * 2, 0, Math.PI * 2)
    ctx.fillStyle = grad
    ctx.fill()

    ctx.shadowBlur = 0
    ctx.beginPath()
    ctx.arc(p.x, p.y, p.r * 0.6, 0, Math.PI * 2)
    ctx.fillStyle = `hsla(${p.hue}, 90%, 90%, ${p.alpha + 0.2})`
    ctx.fill()
  }

  for (let i = 0; i < particles.length; i++) {
    const pi = particles[i]!
    for (let j = i + 1; j < particles.length; j++) {
      const pj = particles[j]!
      const dx = pi.x - pj.x; const dy = pi.y - pj.y
      const dist = Math.sqrt(dx * dx + dy * dy)
      if (dist < 160) {
        const alpha = (1 - dist / 160) * 0.3
        ctx.beginPath()
        ctx.moveTo(pi.x, pi.y)
        ctx.lineTo(pj.x, pj.y)
        ctx.shadowBlur = 8
        ctx.shadowColor = `hsla(${(pi.hue + pj.hue) / 2}, 70%, 60%, ${alpha * 0.3})`
        ctx.strokeStyle = `hsla(${(pi.hue + pj.hue) / 2}, 70%, 65%, ${alpha})`
        ctx.lineWidth = 0.8
        ctx.stroke()
      }
    }
  }

  ctx.shadowBlur = 0

  const md = Math.sqrt((mouse.x - mouse.prevX) ** 2 + (mouse.y - mouse.prevY) ** 2)
  mouse.speed = mouse.speed * 0.9 + md * 0.1
  mouse.prevX = mouse.x; mouse.prevY = mouse.y

  for (const p of particles) {
    const dx = mouse.x - p.x; const dy = mouse.y - p.y
    const dist = Math.sqrt(dx * dx + dy * dy)
    if (dist < 250) {
      const force = (1 - dist / 250) * 1.2 * (1 + mouse.speed * 0.05)
      p.vx -= (dx / dist) * force * 0.03
      p.vy -= (dy / dist) * force * 0.03
    }
    p.vx *= 0.995; p.vy *= 0.995
  }

  rafId = requestAnimationFrame(() => draw(cvs))
}

function onMouseMove(e: MouseEvent) {
  mouse.x = e.clientX; mouse.y = e.clientY
}
function onMouseLeave() {
  mouse.x = -9999; mouse.y = -9999
}

onMounted(() => {
  const cvs = canvasRef.value
  if (!cvs) return
  init(cvs)
  draw(cvs)
  const onResize = () => resize(cvs)
  window.addEventListener('resize', onResize)
  window.addEventListener('mousemove', onMouseMove)
  window.addEventListener('mouseleave', onMouseLeave)
})

onBeforeUnmount(() => {
  cancelAnimationFrame(rafId)
})
</script>

<template>
  <canvas ref="canvasRef" class="particles-canvas" />
</template>

<style scoped>
.particles-canvas {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}
</style>
