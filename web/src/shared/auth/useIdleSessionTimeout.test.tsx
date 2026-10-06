import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, cleanup, render } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { useAuthStore } from '@/shared/auth/store'

import { useIdleSessionTimeout } from './useIdleSessionTimeout'

function Harness({ idleMs }: { idleMs: number }) {
  useIdleSessionTimeout(idleMs)
  return null
}

describe('useIdleSessionTimeout', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    useAuthStore.setState({
      accessToken: 'tok',
      refreshToken: 'refresh',
      role: 'ADMIN',
      userId: 'user-1',
      name: 'Ana',
      email: 'ana@example.com',
    })
  })

  afterEach(() => {
    cleanup()
    vi.useRealTimers()
    useAuthStore.getState().clearSession()
  })

  it('clears the session and the query cache when the operator is idle', () => {
    const client = new QueryClient()
    client.setQueryData(['fields', 'all'], [{ id: 'field-1' }])
    render(
      <QueryClientProvider client={client}>
        <Harness idleMs={1000} />
      </QueryClientProvider>,
    )
    act(() => {
      vi.advanceTimersByTime(1000)
    })
    expect(useAuthStore.getState().accessToken).toBeNull()
    expect(client.getQueryData(['fields', 'all'])).toBeUndefined()
  })
})
