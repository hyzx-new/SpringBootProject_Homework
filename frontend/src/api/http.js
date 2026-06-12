import axios from 'axios'
import { authState, clearSession } from '../stores/auth'

export const http = axios.create({
  baseURL: '/api',
  timeout: 15000
})

http.interceptors.request.use((config) => {
  if (authState.token) {
    config.headers.Authorization = `Bearer ${authState.token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    if (response.data && typeof response.data.success === 'boolean') {
      if (response.data.success) {
        return response.data.data
      }
      throw new Error(response.data.message || '请求失败')
    }
    return response
  },
  (error) => {
    const message = error.response?.data?.message || error.message || '请求失败'
    if (error.response?.status === 401) {
      clearSession()
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(new Error(message))
  }
)
