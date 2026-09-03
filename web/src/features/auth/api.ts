import { apiGet, apiPost } from '@/shared/api/client'
import type { MeResponse, TokenResponse } from '@/shared/api/types'

export async function loginRequest(email: string, password: string): Promise<TokenResponse> {
  return apiPost<TokenResponse>('/api/v1/auth/login', { email, password }, false)
}

export async function fetchMe(): Promise<MeResponse> {
  return apiGet<MeResponse>('/api/v1/auth/me')
}

export async function logoutRequest(refreshToken: string | null): Promise<void> {
  await apiPost('/api/v1/auth/logout', { refreshToken }, false, { skipRefresh: true })
}
