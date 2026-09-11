import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook } from '@testing-library/react'
import { createElement, type ReactNode } from 'react'
import { describe, expect, it } from 'vitest'

import type { Machine } from '@/shared/api/types'
import { useFormat } from '@/shared/lib/useFormat'

const KNOWN = '9b2296fa-d133-37db-be2f-be69dc802915'

function withClient(client: QueryClient) {
  return ({ children }: { children: ReactNode }) => createElement(QueryClientProvider, { client }, children)
}

describe('useFormat.label', () => {
  it('uses the display fallback when the uuid is not in any cache', () => {
    const client = new QueryClient()
    const { result } = renderHook(() => useFormat(), { wrapper: withClient(client) })
    expect(result.current.label('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Drone 01')).toBe('Drone 01')
    expect(result.current.label('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa')).toBe('aaaaaaaa')
  })

  it('resolves ids through the loaded query caches', () => {
    const client = new QueryClient()
    const machine: Machine = {
      id: KNOWN,
      farmId: 'farm-1',
      name: 'Drone 02',
      type: 'DRONE',
      manufacturer: 'X',
      model: 'Y',
      status: 'IDLE',
    }
    client.setQueryData(['machines', 'all'], [machine])
    const { result } = renderHook(() => useFormat(), { wrapper: withClient(client) })
    expect(result.current.label(KNOWN, 'ignored')).toBe('Drone 02')
  })
})
