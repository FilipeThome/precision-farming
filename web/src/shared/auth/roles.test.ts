import { describe, expect, it } from 'vitest'

import { canApprovePrescriptions, canCreateFarm, canDispatchLoads, canWriteFleet, canWriteMasterData } from '@/shared/auth/roles'

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
    expect(canCreateFarm('FARM_MANAGER')).toBe(false)
    expect(canWriteMasterData('OPERATOR')).toBe(false)
    expect(canWriteFleet('OPERATOR')).toBe(false)
  })

  it('allows fleet writers and farm create for admin', () => {
    expect(canCreateFarm('ADMIN')).toBe(true)
    expect(canWriteMasterData('FARM_MANAGER')).toBe(true)
    expect(canWriteFleet('MAINTENANCE')).toBe(true)
  })
})
