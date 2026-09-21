import { http } from './http'
import type { CurrentUser, LoginResponse } from '@/types/api'

export function loginApi(username: string, password: string) {
  return http.post<LoginResponse>('/api/auth/login', { username, password })
}

export function fetchMeApi() {
  return http.get<CurrentUser>('/api/auth/me')
}
