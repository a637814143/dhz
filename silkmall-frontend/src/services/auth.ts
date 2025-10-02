import api from './api'
import type { AuthUser, LoginPayload } from '../types/auth'

export async function login(payload: LoginPayload) {
  const response = await api.post<AuthUser>('/auth/login', payload)
  const user = response.data as AuthUser & { password?: string }

  if ('password' in user) {
    const { password: _password, ...sanitizedUser } = user
    return sanitizedUser
  }

  return user
}
