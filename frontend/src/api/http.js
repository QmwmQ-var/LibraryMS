import axios from 'axios'
import { ElMessage } from 'element-plus'

const TOKEN_KEY = 'libraryms_token'

/**
 * 全局唯一的 Axios 实例。
 * 生产与开发都只使用相对路径 /api，由 Vite 代理或 Nginx 转发到后端。
 */
const http = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const payload = response.data
    if (!payload || typeof payload.code === 'undefined') {
      return payload
    }
    if (payload.code !== 200) {
      const error = new Error(payload.message || '请求失败')
      error.code = payload.code
      error.payload = payload

      if (payload.code === 401 && !response.config?.url?.includes('/auth/login')) {
        localStorage.removeItem(TOKEN_KEY)
        localStorage.removeItem('libraryms_user')
        if (!window.location.pathname.startsWith('/login')) {
          ElMessage.warning('登录状态已失效，请重新登录')
          window.setTimeout(() => window.location.assign('/login'), 500)
        }
      }
      return Promise.reject(error)
    }
    return payload.data
  },
  (error) => {
    const message =
      error.response?.data?.message ||
      (error.code === 'ECONNABORTED' ? '请求超时，请稍后重试' : '网络异常，请检查服务是否启动')
    const normalized = new Error(message)
    normalized.code = error.response?.status || 0
    return Promise.reject(normalized)
  },
)

export { TOKEN_KEY }
export default http
