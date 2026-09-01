import { describe, expect, it } from 'vitest'

import { canApprovePrescriptions, canDispatchLoads } from '@/shared/auth/roles'

describe('auth roles', () => {
  it('allows managers to approve and dispatch', () => {
    expect(canApprovePrescriptions('ADMIN')).toBe(true)
    expect(canApprovePrescriptions('FARM_MANAGER')).toBe(true)
    expect(canDispatchLoads('ADMIN')).toBe(true)
  })

  it('denies operators and anonymous', () => {
    expect(canApprovePrescriptions('OPERATOR')).toBe(false)
    expect(canApprovePrescriptions(null)).toBe(false)
    expect(canDispatchLoads('MAINTENANCE')).toBe(false)
  })
})
