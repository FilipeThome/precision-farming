import { describe, expect, it } from 'vitest'

import { reportDownloadPath, reportsPdfPath } from '@/features/reports/api'

describe('reportsPdfPath', () => {
  it('omits farmId when unset', () => {
    expect(reportsPdfPath('operations')).toBe('/api/v1/reports/operations.pdf')
  })

  it('sends the farm switcher id', () => {
    const id = 'bbc017bc-be38-34d4-95df-0b1f15162e1d'
    expect(reportsPdfPath('operations', id)).toBe(`/api/v1/reports/operations.pdf?farmId=${id}`)
    expect(reportsPdfPath('inventory', id)).toBe(`/api/v1/reports/inventory.pdf?farmId=${id}`)
  })

  it('appends farmId to catalog paths', () => {
    const id = 'bbc017bc-be38-34d4-95df-0b1f15162e1d'
    expect(reportDownloadPath('/api/v1/reports/operations.pdf', id)).toBe(`/api/v1/reports/operations.pdf?farmId=${id}`)
  })

  it('rejects unexpected download paths', () => {
    expect(() => reportDownloadPath('/api/v1/auth/me')).toThrow('Invalid report download path')
  })
})
