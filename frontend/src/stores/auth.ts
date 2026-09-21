import { defineStore } from 'pinia'
import { loginApi, fetchMeApi } from '@/api/auth'
import { TOKEN_KEY } from '@/api/http'
import type { Role } from '@/types/api'

const USER_KEY = 'cvb_user'

interface AuthState {
  token: string
  userId: number | null
  username: string
  role: string
}

interface StoredUser {
  userId: number
  username: string
  role: string
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: '',
    userId: null,
    username: '',
    role: '',
  }),
  getters: {
    isLoggedIn: (s) => !!s.token,
    isAdmin: (s) => s.role === 'ADMIN',
  },
  actions: {
    hydrateFromStorage() {
      const token = localStorage.getItem(TOKEN_KEY) || ''
      this.token = token
      const raw = localStorage.getItem(USER_KEY)
      if (raw) {
        try {
          const u = JSON.parse(raw) as StoredUser
          this.userId = u.userId
          this.username = u.username
          this.role = u.role
        } catch {
          localStorage.removeItem(USER_KEY)
        }
      }
    },
    persistUser() {
      if (this.token) {
        localStorage.setItem(TOKEN_KEY, this.token)
        localStorage.setItem(
          USER_KEY,
          JSON.stringify({
            userId: this.userId,
            username: this.username,
            role: this.role,
          }),
        )
      }
    },
    clear() {
      this.token = ''
      this.userId = null
      this.username = ''
      this.role = ''
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    },
    async login(username: string, password: string) {
      const data = await loginApi(username, password)
      this.token = data.token
      this.userId = data.userId
      this.username = data.username
      this.role = data.role
      this.persistUser()
      return data
    },
    logout() {
      this.clear()
    },
    async fetchMe() {
      const me = await fetchMeApi()
      this.userId = me.userId
      this.username = me.username
      this.role = me.role as Role
      this.persistUser()
      return me
    },
  },
})
