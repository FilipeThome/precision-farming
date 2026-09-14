import { describe, expect, it } from 'vitest'

import { decisionHref, decisionsHref } from '@/features/decisions/components/DecisionRow'
import type { DecisionItem } from '@/features/decisions/model'

const item: DecisionItem = {
  id: 'PRESCRIPTION:p1',
  rawId: 'p1',
  source: 'PRESCRIPTION',
  title: 'GLYPHOSATE',
  farmId: 'farm-3',
  status: 'PENDING',
  capabilities: { approve: true, simulate: false },
}

describe('decisionsHref', () => {
  it('keeps farm and non-default status when a row is selected', () => {
    expect(decisionsHref({ id: item.id, farmId: item.farmId, status: 'approved' })).toBe(
      '/decisions/PRESCRIPTION%3Ap1?farm=farm-3&status=approved',
    )
    expect(decisionHref(item, 'all')).toBe('/decisions/PRESCRIPTION%3Ap1?farm=farm-3&status=all')
  })

  it('omits pending (the default) so the filter does not snap back', () => {
    expect(decisionHref(item, 'pending')).toBe('/decisions/PRESCRIPTION%3Ap1?farm=farm-3')
    expect(decisionsHref({ farmId: 'farm-3', status: 'pending' })).toBe('/decisions?farm=farm-3')
  })

  it('preserves status when changing filter while a decision is open', () => {
    expect(decisionsHref({ id: item.id, farmId: 'farm-3', status: 'all' })).toBe(
      '/decisions/PRESCRIPTION%3Ap1?farm=farm-3&status=all',
    )
  })
})
