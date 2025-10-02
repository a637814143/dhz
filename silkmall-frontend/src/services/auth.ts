import api from './api'
import type {
  ApiResponse,
  CaptchaChallenge,
  LoginPayload,
  LoginResponse,
  RegisterPayload,
} from '@/types'

export async function requestCaptcha() {
  const { data } = await api.get<ApiResponse<CaptchaChallenge>>('/auth/captcha')
  return data.data
}

export async function login(payload: LoginPayload) {
  const { data } = await api.post<ApiResponse<LoginResponse>>('/auth/login', payload)
  return data.data
}

export async function register(payload: RegisterPayload) {
  const { data } = await api.post<ApiResponse<null>>('/auth/register', payload)
  return data.message
}

export async function requestPasswordReset(email: string) {
  const { data } = await api.post<ApiResponse<string>>('/auth/forgot-password', null, {
    params: { email },
  })
  return data.data
}
