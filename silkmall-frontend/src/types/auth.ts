export type UserRole = 'consumer' | 'supplier' | 'admin'

export interface AuthUser {
  id: number
  username: string
  role: UserRole | string
  email?: string | null
  phone?: string | null
  address?: string | null
  profile?: Record<string, unknown>
  [key: string]: unknown
}

export interface LoginPayload {
  username: string
  password: string
}
