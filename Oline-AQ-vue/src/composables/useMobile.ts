import { computed, onMounted, onUnmounted, ref } from 'vue'

const MOBILE_WIDTH = 980

export function useMobile() {
  const width = ref(typeof window === 'undefined' ? MOBILE_WIDTH + 1 : window.innerWidth)

  function syncWidth() {
    width.value = window.innerWidth
  }

  onMounted(() => {
    syncWidth()
    window.addEventListener('resize', syncWidth, { passive: true })
  })

  onUnmounted(() => {
    window.removeEventListener('resize', syncWidth)
  })

  return {
    width,
    isMobile: computed(() => width.value <= MOBILE_WIDTH),
  }
}
