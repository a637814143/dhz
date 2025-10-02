import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  timeout: 10000,
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    let message: string | undefined

    const responseData = error?.response?.data
    if (typeof responseData === 'string') {
      message = responseData
    } else if (responseData && typeof responseData === 'object' && 'message' in responseData) {
      const dataMessage = (responseData as { message?: unknown }).message
      if (typeof dataMessage === 'string') {
        message = dataMessage
      }
    }

    if (!message) {
      message = error?.message || '请求失败，请稍后重试'
    }

    return Promise.reject(new Error(message))
  }
)

export default api
