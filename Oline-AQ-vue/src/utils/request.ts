import axios from 'axios'

const TOKEN_STORAGE_KEY = 'token'

interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

function clearAuthState() {
  localStorage.removeItem(TOKEN_STORAGE_KEY)
  localStorage.removeItem('user')
}

const pendingMap = new Map<string, AbortController>()

function getRequestKey(config: { method?: string; url?: string; params?: unknown; data?: unknown }): string {
  return `${config.method || ''}:${config.url || ''}:${JSON.stringify(config.params || config.data || '')}`
}

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_STORAGE_KEY)
  if (token) {
    config.headers = config.headers ?? {}
    config.headers.Authorization = `Bearer ${token}`
  }

  if (config.method?.toLowerCase() === 'get') {
    const key = getRequestKey(config)
    const prev = pendingMap.get(key)
    if (prev) {
      prev.abort()
    }
    const controller = new AbortController()
    config.signal = controller.signal
    pendingMap.set(key, controller)
    Object.assign(config, { _dedupKey: key })
  }

  return config
})

request.interceptors.response.use(
  (response) => {
    const dedupKey = (response.config as any)._dedupKey
    if (dedupKey) {
      pendingMap.delete(dedupKey)
    }

    const payload = response.data as ApiResponse

    if (!payload || typeof payload.code !== 'number') {
      return response.data
    }

    if (payload.code !== 200) {
      return Promise.reject(new Error(payload.message || '请求失败'))
    }

    return payload
  },
  (error) => {
    const dedupKey = error.config?._dedupKey
    if (dedupKey) {
      pendingMap.delete(dedupKey)
    }

    if (axios.isCancel(error)) {
      return Promise.reject(new Error('请求已取消'))
    }

    if (error.response?.data instanceof Blob) {
      return Promise.reject(error)
    }

    const serverMessage = error.response?.data?.message
    if (serverMessage) {
      return Promise.reject(new Error(serverMessage))
    }

    if (error.response?.status === 401) {
      clearAuthState()
      window.location.href = '/login'
      return Promise.reject(new Error('登录已过期，请重新登录'))
    }

    return Promise.reject(error)
  },
)

export default request
