import axios from 'axios'
import { useAuthState } from './authState'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  timeout: 10000,
})

const { token, clearAuth } = useAuthState()

api.interceptors.request.use((config) => {
  if (token.value) {
    config.headers = config.headers ?? {}
    config.headers.Authorization = `Bearer ${token.value}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status
    if (status === 401 || status === 403) {
      clearAuth()
      const current = window.location.pathname + window.location.search
      if (!current.startsWith('/auth')) {
        window.location.href = `/auth?mode=login&redirect=${encodeURIComponent(current)}`
      }
    }
    const data = error?.response?.data
    const message =
      (typeof data === 'string' && data.trim().length > 0 ? data : data?.message) ||
      error.message ||
      '请求失败，请稍后重试'
    return Promise.reject(new Error(message))
  }
)

export default api
