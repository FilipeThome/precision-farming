import { describe, expect, it } from 'vitest'

import type { HarvestPlan, YieldRecord } from '@/shared/api/types'

import { weightedYieldTHa, yieldsForPlan } from './yieldMath'

describe('weightedYieldTHa', () => {
  it('returns 0 when empty', () => {
    expect(weightedYieldTHa([])).toBe(0)
  })

  it('averages rates when area is missing', () => {
    expect(weightedYieldTHa([{ yieldTHa: 4 }, { yieldTHa: 6 }])).toBe(5)
  })

  it('weights by area when present', () => {
    expect(
      weightedYieldTHa([
        { yieldTHa: 2, areaHa: 10 },
        { yieldTHa: 4, areaHa: 30 },
      ]),
    ).toBe(3.5)
  })
})

describe('yieldsForPlan', () => {
  const plan = { id: 'p1', fieldId: 'f1' } as HarvestPlan

  it('keeps plan rows and unscoped field rows', () => {
    const rows = [
      { id: 'a', fieldId: 'f1', planId: 'p1', yieldTHa: 3 },
      { id: 'b', fieldId: 'f1', planId: null, yieldTHa: 4 },
      { id: 'c', fieldId: 'f1', planId: 'p2', yieldTHa: 9 },
    ] as YieldRecord[]
    expect(yieldsForPlan(rows, plan).map((row) => row.id)).toEqual(['a', 'b'])
  })
})
