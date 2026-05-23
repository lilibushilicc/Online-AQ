import { onUnmounted, ref } from 'vue'
import { ElNotification } from 'element-plus'

const eventSource = ref<EventSource | null>(null)

export function useNotification() {
  function connect() {
    const token = localStorage.getItem('token')
    if (!token) return

    const base = import.meta.env.VITE_API_BASE_URL || '/api'
    const url = `${base}/notifications/subscribe?token=${encodeURIComponent(token)}`

    const es = new EventSource(url)
    eventSource.value = es

    es.addEventListener('exam_published', (e) => {
      try {
        const data = JSON.parse(e.data)
        ElNotification({
          title: '新考试发布',
          message: data.message,
          type: 'success',
          duration: 6000,
        })
      } catch { /* ignore */ }
    })

    es.addEventListener('announcement', (e) => {
      try {
        const data = JSON.parse(e.data)
        ElNotification({
          title: '新公告',
          message: data.message,
          type: 'info',
          duration: 6000,
        })
      } catch { /* ignore */ }
    })

    es.onerror = () => {
      es.close()
      eventSource.value = null
      setTimeout(connect, 10000)
    }
  }

  function disconnect() {
    if (eventSource.value) {
      eventSource.value.close()
      eventSource.value = null
    }
  }

  onUnmounted(disconnect)

  return { connect, disconnect }
}
