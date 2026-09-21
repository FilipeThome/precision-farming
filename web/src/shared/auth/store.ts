import { create } from 'zustand'

import type { TokenResponse } from '@/shared/api/types'

type AuthState = {
  accessToken: string | null
  refreshToken: string | null
  role: string | null
  userId: string | null
  name: string | null
  email: string | null
  setSession: (session: TokenResponse) => void
  clearRefresh: () => void
  clearSession: () => void
}

export const useAuthStore = create<AuthState>()((set) => ({
  accessToken: null,
  refreshToken: null,
  role: null,
  userId: null,
  name: null,
  email: null,
  setSession: (session) =>
    set({
      accessToken: session.accessToken,
      refreshToken: session.refreshToken,
      role: session.role,
      userId: session.userId,
      name: session.name,
      email: session.email,
    }),
  clearRefresh: () => set({ refreshToken: null }),
  clearSession: () =>
    set({
      accessToken: null,
      refreshToken: null,
      role: null,
      userId: null,
      name: null,
      email: null,
    }),
}))
