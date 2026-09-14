import { describe, expect, it } from 'vitest'

import type { DecisionItem } from '@/features/decisions/model'
import { actionableDecisions, filterDecisions, pendingDecisions, sortByPriority } from '@/features/decisions/selectors'

function item(over: Partial<DecisionItem> & { id: string }): DecisionItem {
  return {
    rawId: over.id,
    source: 'PRESCRIPTION',
    title: 'X',
    status: 'UNKNOWN',
    capabilities: { approve: false, simulate: false },
    ...over,
  }
}

const pendingLow = item({ id: 'a', status: 'PENDING', priority: 'LOW', createdAt: '2026-09-01T00:00:00Z' })
const pendingHigh = item({ id: 'b', status: 'PENDING', priority: 'HIGH', createdAt: '2026-09-02T00:00:00Z' })
const approved = item({ id: 'c', status: 'APPROVED', createdAt: '2026-09-03T00:00:00Z' })
const lowConf = item({ id: 'd', source: 'AI_INSIGHT', confidence: 0.4, createdAt: '2026-09-04T00:00:00Z' })
const highConf = item({ id: 'e', source: 'AI_INSIGHT', confidence: 0.9, createdAt: '2026-09-05T00:00:00Z' })
const newestPending = item({ id: 'f', status: 'PENDING', createdAt: '2026-09-06T00:00:00Z' })

describe('decision selectors', () => {
  it('pendingDecisions keeps only PENDING items', () => {
    expect(pendingDecisions([pendingLow, approved, lowConf, pendingHigh]).map((i) => i.id)).toEqual(['a', 'b'])
  })

  it('actionableDecisions adds low-confidence insights', () => {
    expect(actionableDecisions([approved, lowConf, highConf, pendingLow]).map((i) => i.id)).toEqual(['d', 'a'])
  })

  it('sortByPriority orders by priority, then status, then confidence, then newest', () => {
    const sorted = sortByPriority([approved, pendingLow, lowConf, highConf, pendingHigh, newestPending])
    expect(sorted.map((i) => i.id)).toEqual(['b', 'a', 'f', 'd', 'e', 'c'])
  })

  it('filterDecisions supports pending / approved / all', () => {
    const all = [pendingLow, approved, lowConf]
    expect(filterDecisions(all, 'pending')).toHaveLength(1)
    expect(filterDecisions(all, 'approved')).toHaveLength(1)
    expect(filterDecisions(all, 'all')).toHaveLength(3)
  })

  it('pendingDecisions ignores low-confidence insights and UNKNOWN statuses (TrustStrip count)', () => {
    expect(pendingDecisions([lowConf, highConf, item({ id: 'u', status: 'UNKNOWN' })])).toEqual([])
    expect(pendingDecisions([])).toEqual([])
  })

  it('filterDecisions "approved" includes executed items and excludes rejected', () => {
    const executed = item({ id: 'x', status: 'EXECUTED' })
    const rejected = item({ id: 'r', status: 'REJECTED' })
    expect(filterDecisions([approved, executed, rejected, pendingLow], 'approved').map((i) => i.id)).toEqual(['c', 'x'])
    expect(filterDecisions([approved, executed, rejected, pendingLow], 'all')).toHaveLength(4)
  })

  it('sortByPriority is case-insensitive on priority and ranks unknown priorities after LOW', () => {
    const lower = item({ id: 'lc', status: 'PENDING', priority: 'critical' })
    const weird = item({ id: 'w', status: 'PENDING', priority: 'URGENTISH' })
    const none = item({ id: 'n', status: 'PENDING' })
    const sorted = sortByPriority([none, weird, pendingLow, lower]).map((i) => i.id)
    expect(sorted.slice(0, 2)).toEqual(['lc', 'a'])
    expect(sorted.slice(2).sort()).toEqual(['n', 'w'])
  })

  it('sortByPriority orders statuses PENDING > UNKNOWN > APPROVED > EXECUTED > REJECTED for equal priority', () => {
    const items = [
      item({ id: 'rej', status: 'REJECTED' }),
      item({ id: 'exe', status: 'EXECUTED' }),
      item({ id: 'app', status: 'APPROVED' }),
      item({ id: 'unk', status: 'UNKNOWN' }),
      item({ id: 'pen', status: 'PENDING' }),
    ]
    expect(sortByPriority(items).map((i) => i.id)).toEqual(['pen', 'unk', 'app', 'exe', 'rej'])
  })

  it('sortByPriority puts low confidence before high confidence even when older', () => {
    const olderLow = item({ id: 'ol', source: 'AI_INSIGHT', confidence: 0.2, createdAt: '2026-01-01T00:00:00Z' })
    const newerHigh = item({ id: 'nh', source: 'AI_INSIGHT', confidence: 0.95, createdAt: '2026-09-01T00:00:00Z' })
    expect(sortByPriority([newerHigh, olderLow]).map((i) => i.id)).toEqual(['ol', 'nh'])
  })

  it('sortByPriority puts items without createdAt after dated ones and does not mutate the input', () => {
    const undated = item({ id: 'u', status: 'PENDING' })
    const dated = item({ id: 'd', status: 'PENDING', createdAt: '2026-09-01T00:00:00Z' })
    const input = [undated, dated]
    const sorted = sortByPriority(input)
    expect(sorted.map((i) => i.id)).toEqual(['d', 'u'])
    expect(input.map((i) => i.id)).toEqual(['u', 'd'])
    expect(sorted).not.toBe(input)
  })

  it('actionableDecisions keeps a low-confidence insight even when it is not pending', () => {
    const approvedLow = item({ id: 'al', status: 'APPROVED', confidence: 0.1 })
    expect(actionableDecisions([approvedLow, highConf])).toEqual([approvedLow])
    expect(actionableDecisions([])).toEqual([])
  })
})
