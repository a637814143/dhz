import { computed, reactive } from 'vue'
import type { AuthUser, LoginResponse, UserRole } from '@/types'

interface AuthState {
  token: string | null
  user: AuthUser | null
  expiresAt: number | null
  redirectUrl: string | null
  guest: boolean
}

const STORAGE_KEY = 'silkmall.auth'
const GUEST_STORAGE_KEY = 'silkmall.guest-mode'

const state = reactive<AuthState>({
  token: null,
  user: null,
  expiresAt: null,
  redirectUrl: null,
  guest: false,
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

  try {
    const guestFlag = sessionStorage.getItem(GUEST_STORAGE_KEY)
    state.guest = guestFlag === '1'
  } catch (error) {
    console.warn('无法读取游客模式状态', error)
    state.guest = false
  }
}

loadFromStorage()

const isAuthenticated = computed(() => {
  if (!state.token || !state.expiresAt) return false
  return Date.now() < state.expiresAt
})

const token = computed(() => (isAuthenticated.value ? state.token : null))

const isGuestSession = computed(() => !isAuthenticated.value && state.guest)

function persistGuestState(enabled: boolean) {
  state.guest = enabled
  try {
    if (enabled) {
      sessionStorage.setItem(GUEST_STORAGE_KEY, '1')
    } else {
      sessionStorage.removeItem(GUEST_STORAGE_KEY)
    }
  } catch (error) {
    console.warn('无法保存游客模式状态', error)
  }
}

function resetAuthState() {
  state.token = null
  state.user = null
  state.expiresAt = null
  state.redirectUrl = null
}

function setAuth(response: LoginResponse) {
  const expiresAt = response.issuedAt + response.expiresIn * 1000
  state.token = response.token
  state.user = response.user
  state.expiresAt = expiresAt
  state.redirectUrl = response.redirectUrl
  persistGuestState(false)
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
  resetAuthState()
  persistGuestState(false)
  localStorage.removeItem(STORAGE_KEY)
}

function enterGuestMode() {
  localStorage.removeItem(STORAGE_KEY)
  resetAuthState()
  persistGuestState(true)
}

function exitGuestMode() {
  if (!state.guest) return
  persistGuestState(false)
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
    isGuestSession,
    enterGuestMode,
    exitGuestMode,
  }
}
