import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResult } from '@/types/api'

const TOKEN_KEY = 'cvb_token'

const instance: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  timeout: 15000,
})

instance.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers = config.headers ?? {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

function redirectToLogin() {
  void import('@/router').then(({ default: router }) => {
    const current = router.currentRoute.value
    if (current.path !== '/login') {
      void router.push({ path: '/login', query: { redirect: current.fullPath } })
    }
  })
}

instance.interceptors.response.use(
  (response) => {
    const payload = response.data as ApiResult<unknown>
    if (payload && typeof payload === 'object' && 'code' in payload) {
      if (payload.code !== 'OK') {
        return Promise.reject(new Error(payload.message || payload.code || '请求失败'))
      }
      return payload.data as never
    }
    return response.data as never
  },
  (error) => {
    const status = error?.response?.status as number | undefined
    const data = error?.response?.data as ApiResult<unknown> | undefined
    const message = data?.message || error?.message || '网络错误'

    if (status === 401) {
      // Clear Pinia + storage before navigation so /login guard won't bounce back
      void import('@/stores/auth').then(({ useAuthStore }) => {
        useAuthStore().clear()
        redirectToLogin()
      })
      ElMessage.error(message || '未登录或登录已失效')
      return Promise.reject(new Error(message))
    }
    if (status === 403) {
      ElMessage.error(message || '无权限')
      return Promise.reject(new Error(message))
    }
    return Promise.reject(new Error(message))
  },
)

export const http = {
  get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return instance.get(url, config) as Promise<T>
  },
  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return instance.post(url, data, config) as Promise<T>
  },
  put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return instance.put(url, data, config) as Promise<T>
  },
}

export { TOKEN_KEY }
