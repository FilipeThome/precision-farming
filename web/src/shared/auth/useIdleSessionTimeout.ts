import { useQueryClient } from '@tanstack/react-query'
import { useEffect } from 'react'

import { useAuthStore } from '@/shared/auth/store'

/** Clears web session after idle period (default 30 minutes). */
const IDLE_MS = 30 * 60 * 1000
const ACTIVITY_EVENTS = ['mousemove', 'mousedown', 'keydown', 'touchstart', 'scroll', 'visibilitychange'] as const

export function useIdleSessionTimeout(idleMs: number = IDLE_MS) {
  const token = useAuthStore((s) => s.accessToken)
  const clearSession = useAuthStore((s) => s.clearSession)
  const queryClient = useQueryClient()

  useEffect(() => {
    if (!token) return

    let timer: ReturnType<typeof setTimeout> | undefined

    const reset = () => {
      if (timer) clearTimeout(timer)
      timer = setTimeout(() => {
        clearSession()
        queryClient.clear()
      }, idleMs)
    }

    reset()
    for (const ev of ACTIVITY_EVENTS) {
      window.addEventListener(ev, reset, { passive: true })
    }
    return () => {
      if (timer) clearTimeout(timer)
      for (const ev of ACTIVITY_EVENTS) {
        window.removeEventListener(ev, reset)
      }
    }
  }, [token, clearSession, idleMs, queryClient])
}
