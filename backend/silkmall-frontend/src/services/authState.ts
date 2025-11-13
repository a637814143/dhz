import { computed, reactive } from 'vue'
import type { AuthUser, LoginResponse, UserRole } from '@/types'

interface AuthState {
  token: string | null
  user: AuthUser | null
  expiresAt: number | null
  redirectUrl: string | null
}

const STORAGE_KEY = 'silkmall.auth'

const state = reactive<AuthState>({
  token: null,
  user: null,
  expiresAt: null,
  redirectUrl: null,
})

function loadFromStorage() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return
    const payload = JSON.parse(raw) as AuthState
    if (payload.token && payload.user && payload.expiresAt && Date.now() < payload.expiresAt) {
      state.token = payload.token
      state.user = payload.user
      state.expiresAt = payload.expiresAt
      state.redirectUrl = payload.redirectUrl ?? null
    } else {
      localStorage.removeItem(STORAGE_KEY)
    }
  } catch (error) {
    console.warn('无法解析本地认证信息', error)
    localStorage.removeItem(STORAGE_KEY)
  }
}

loadFromStorage()

const isAuthenticated = computed(() => {
  if (!state.token || !state.expiresAt) return false
  return Date.now() < state.expiresAt
})

const token = computed(() => (isAuthenticated.value ? state.token : null))

function setAuth(response: LoginResponse) {
  const expiresAt = response.issuedAt + response.expiresIn * 1000
  state.token = response.token
  state.user = response.user
  state.expiresAt = expiresAt
  state.redirectUrl = response.redirectUrl
  localStorage.setItem(
    STORAGE_KEY,
    JSON.stringify({
      token: state.token,
      user: state.user,
      expiresAt: state.expiresAt,
      redirectUrl: state.redirectUrl,
    })
  )
}

function clearAuth() {
  state.token = null
  state.user = null
  state.expiresAt = null
  state.redirectUrl = null
  localStorage.removeItem(STORAGE_KEY)
}

function hasRole(role: UserRole) {
  return state.user?.userType === role
}

export function useAuthState() {
  return {
    state,
    isAuthenticated,
    token,
    setAuth,
    clearAuth,
    hasRole,
  }
}
