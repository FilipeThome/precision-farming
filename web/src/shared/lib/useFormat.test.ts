import { renderHook } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { useFormat } from '@/shared/lib/useFormat'

describe('useFormat.label', () => {
  it('uses the display fallback when the uuid is unknown', () => {
    const { result } = renderHook(() => useFormat())
    expect(result.current.label('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Drone 01')).toBe('Drone 01')
    expect(result.current.label('9b2296fa-d133-37db-be2f-be69dc802915', 'ignored')).toBe('Drone 02')
  })
})
