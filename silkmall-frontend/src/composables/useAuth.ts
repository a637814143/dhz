import { computed, ref } from 'vue'
import type { AuthUser } from '../types/auth'

const STORAGE_KEY = 'silkmall-auth-user'

function loadStoredUser(): AuthUser | null {
  if (typeof window === 'undefined') {
    return null
  }

  const raw = window.localStorage.getItem(STORAGE_KEY)
  if (!raw) {
    return null
  }

  try {
    return JSON.parse(raw) as AuthUser
  } catch (error) {
    console.warn('[SilkMall] 无法解析本地缓存的用户信息', error)
    window.localStorage.removeItem(STORAGE_KEY)
    return null
  }
}

const authUser = ref<AuthUser | null>(loadStoredUser())

function persistUser(user: AuthUser | null) {
  if (typeof window === 'undefined') {
    return
  }

  if (user) {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(user))
  } else {
    window.localStorage.removeItem(STORAGE_KEY)
  }
}

export function setAuthUser(user: AuthUser | null) {
  authUser.value = user
  persistUser(user)
}

export function getAuthUser(): AuthUser | null {
  if (!authUser.value) {
    authUser.value = loadStoredUser()
  }
  return authUser.value
}

export function isAuthenticated(): boolean {
  return getAuthUser() !== null
}

export function useAuth() {
  const user = computed(() => authUser.value)
  const authenticated = computed(() => user.value !== null)

  const logout = () => {
    setAuthUser(null)
  }

  return {
    user,
    isAuthenticated: authenticated,
    setUser: setAuthUser,
    logout,
  }
}
