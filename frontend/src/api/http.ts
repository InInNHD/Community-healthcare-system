import axios from 'axios'
import { ElMessage } from 'element-plus'

export const http = axios.create({
  baseURL: '/api', timeout: 15000, withCredentials: true, withXSRFToken: true,
  xsrfCookieName: 'XSRF-TOKEN', xsrfHeaderName: 'X-XSRF-TOKEN',
})

http.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('healthcare_user')
      if (location.pathname !== '/login') location.href = '/login'
    } else {
      ElMessage.error(error.response?.data?.message ?? '服务暂时不可用，请稍后重试')
    }
    return Promise.reject(error)
  },
)

export interface PageResult<T = Record<string, unknown>> {
  items: T[]
  total: number
  page: number
  size: number
  totalPages: number
}
