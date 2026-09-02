import { describe, expect, it } from 'vitest'

import { reportsCsvPath } from '@/features/reports/api'

describe('reportsCsvPath', () => {
  it('omits farmId when unset', () => {
    expect(reportsCsvPath('operations')).toBe('/api/v1/reports/operations.csv')
  })

  it('sends the farm switcher id', () => {
    const id = 'bbc017bc-be38-34d4-95df-0b1f15162e1d'
    expect(reportsCsvPath('operations', id)).toBe(`/api/v1/reports/operations.csv?farmId=${id}`)
    expect(reportsCsvPath('inventory', id)).toBe(`/api/v1/reports/inventory.csv?farmId=${id}`)
  })
})
