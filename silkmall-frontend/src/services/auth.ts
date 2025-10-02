import api from './api'
import type { AuthUser, LoginPayload } from '../types/auth'

export async function login(payload: LoginPayload) {
  const response = await api.post<AuthUser>('/auth/login', payload)
  return response.data
}
