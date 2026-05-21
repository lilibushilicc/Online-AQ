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
  return config
})

request.interceptors.response.use(
  (response) => {
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
    // Blob 响应（文件导出请求）的错误：不尝试解析 message
    if (error.response?.data instanceof Blob) {
      return Promise.reject(error)
    }

    // 优先提取后端返回的业务错误信息
    const serverMessage = error.response?.data?.message
    if (serverMessage) {
      return Promise.reject(new Error(serverMessage))
    }

    // 401 未授权（token 过期/无效），清除登录状态
    if (error.response?.status === 401) {
      clearAuthState()
      window.location.href = '/login'
      return Promise.reject(new Error('登录已过期，请重新登录'))
    }

    return Promise.reject(error)
  },
)

export default request
