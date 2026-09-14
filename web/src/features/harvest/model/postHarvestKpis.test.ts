import { describe, expect, it } from 'vitest'

import {
  expectedTons,
  flowStages,
  harvestedTons,
  loadsSummary,
  qualityBreakdown,
  storageOccupancyTotal,
  unitOccupancyPct,
} from '@/features/harvest/model/postHarvestKpis'
import type { Field } from '@/shared/api/types'

const field: Field = { id: 'f1', farmId: 'farm', name: 'A', areaHa: 10, crop: 'SOY', variety: null, geometry: '' }

describe('postHarvestKpis', () => {
  it('computes expected tons only for plans whose field has an area', () => {
    const out = expectedTons(
      [
        { id: 'p1', fieldId: 'f1', expectedTHa: 3.5 },
        { id: 'p2', fieldId: 'missing', expectedTHa: 4 },
        { id: 'p3', fieldId: 'f1' },
      ],
      [field],
    )
    expect(out).toEqual({ tons: 35, withArea: 1, total: 3 })
  })

  it('computes harvested tons from records with area', () => {
    expect(harvestedTons([{ id: 'y1', yieldTHa: 3, areaHa: 5 }, { id: 'y2', yieldTHa: 2 }])).toEqual({
      tons: 15,
      withArea: 1,
      total: 2,
    })
  })

  it('summarises loads by status', () => {
    expect(
      loadsSummary([
        { id: 'l1', status: 'QUEUED', tons: 10 },
        { id: 'l2', status: 'DISPATCHED', tons: 20 },
        { id: 'l3', status: 'DELIVERED', tons: 30 },
      ]),
    ).toEqual({ queued: 1, inTransit: 1, delivered: 1, total: 3, tonsInTransit: 20 })
  })

  it('computes storage occupancy', () => {
    expect(storageOccupancyTotal([{ id: 's1', usedT: 50, capacityT: 100 }, { id: 's2', usedT: 25, capacityT: 100 }])).toEqual({
      usedT: 75,
      capacityT: 200,
      pct: 38,
    })
    expect(storageOccupancyTotal([])).toEqual({ usedT: 0, capacityT: 0, pct: null })
    expect(unitOccupancyPct({ id: 's', usedT: 120, capacityT: 100 })).toBe(100)
    expect(unitOccupancyPct({ id: 's', usedT: 1 })).toBeNull()
  })

  it('groups lots by quality sorted by tons', () => {
    const rows = qualityBreakdown([
      { id: 'a', unitId: 'u', farmId: 'f', crop: 'SOY', tons: 10, quality: 'A', receivedAt: '' },
      { id: 'b', unitId: 'u', farmId: 'f', crop: 'SOY', tons: 30, quality: 'B', receivedAt: '' },
      { id: 'c', unitId: 'u', farmId: 'f', crop: 'SOY', tons: 5, quality: 'A', receivedAt: '' },
    ])
    expect(rows).toEqual([
      { quality: 'B', count: 1, tons: 30 },
      { quality: 'A', count: 2, tons: 15 },
    ])
  })

  describe('expectedTons edge cases', () => {
    it('skips plans without fieldId or expectedTHa and reports coverage n/m', () => {
      const out = expectedTons([{ id: 'p1', expectedTHa: 3 }, { id: 'p2', fieldId: 'f1' }, { id: 'p3', fieldId: 'f1', expectedTHa: 2 }], [field])
      expect(out).toEqual({ tons: 20, withArea: 1, total: 3 })
    })

    it('is zero-covered when no field has an area', () => {
      expect(expectedTons([{ id: 'p1', fieldId: 'f1', expectedTHa: 3 }], [])).toEqual({ tons: 0, withArea: 0, total: 1 })
      expect(expectedTons([{ id: 'p1', fieldId: 'f1', expectedTHa: 3 }], [{ ...field, areaHa: null as unknown as number }])).toEqual({
        tons: 0,
        withArea: 0,
        total: 1,
      })
    })

    it('counts a zero expected yield as measured (0 t)', () => {
      expect(expectedTons([{ id: 'p1', fieldId: 'f1', expectedTHa: 0 }], [field])).toEqual({ tons: 0, withArea: 1, total: 1 })
    })

    it('handles empty inputs', () => {
      expect(expectedTons([], [field])).toEqual({ tons: 0, withArea: 0, total: 0 })
      expect(harvestedTons([])).toEqual({ tons: 0, withArea: 0, total: 0 })
    })
  })

  describe('harvestedTons edge cases', () => {
    it('skips records with a null area or missing yield', () => {
      expect(harvestedTons([{ id: 'y1', yieldTHa: 3, areaHa: null }, { id: 'y2', areaHa: 5 }])).toEqual({ tons: 0, withArea: 0, total: 2 })
    })

    it('accumulates fractional values', () => {
      expect(harvestedTons([{ id: 'y1', yieldTHa: 3.5, areaHa: 2 }, { id: 'y2', yieldTHa: 1.25, areaHa: 4 }]).tons).toBe(12)
    })
  })

  describe('loadsSummary edge cases', () => {
    it('is case-insensitive and maps every status alias', () => {
      const out = loadsSummary([
        { id: 'a', status: 'queued', tons: 1 },
        { id: 'b', status: 'PLANNED', tons: 1 },
        { id: 'c', status: 'IN_TRANSIT', tons: 2 },
        { id: 'd', status: 'in_progress', tons: 3 },
        { id: 'e', status: 'RECEIVED', tons: 4 },
        { id: 'f', status: 'COMPLETED', tons: 4 },
      ])
      expect(out).toEqual({ queued: 2, inTransit: 2, delivered: 2, total: 6, tonsInTransit: 5 })
    })

    it('counts unknown or missing statuses only in total and treats missing tons as 0', () => {
      const out = loadsSummary([{ id: 'a' }, { id: 'b', status: 'CANCELLED', tons: 9 }, { id: 'c', status: 'DISPATCHED' }])
      expect(out).toEqual({ queued: 0, inTransit: 1, delivered: 0, total: 3, tonsInTransit: 0 })
      expect(loadsSummary([])).toEqual({ queued: 0, inTransit: 0, delivered: 0, total: 0, tonsInTransit: 0 })
    })
  })

  describe('storage occupancy with zero or missing capacity', () => {
    it('total occupancy is "—" when the summed capacity is 0 even if something is stored', () => {
      expect(storageOccupancyTotal([{ id: 's1', usedT: 10, capacityT: 0 }, { id: 's2', usedT: 5 }])).toEqual({
        usedT: 15,
        capacityT: 0,
        pct: null,
      })
    })

    it('unit occupancy is null for zero/negative/missing capacity, 0 for empty units, clamped at 100', () => {
      expect(unitOccupancyPct({ id: 's', usedT: 10, capacityT: 0 })).toBeNull()
      expect(unitOccupancyPct({ id: 's', usedT: 10, capacityT: -5 })).toBeNull()
      expect(unitOccupancyPct({ id: 's', capacityT: 100 })).toBe(0)
      expect(unitOccupancyPct({ id: 's', usedT: 33.4, capacityT: 100 })).toBe(33)
      expect(unitOccupancyPct({ id: 's', usedT: 100, capacityT: 100 })).toBe(100)
    })

    it('ignores units with missing fields when summing', () => {
      expect(storageOccupancyTotal([{ id: 's1' }, { id: 's2', usedT: 20, capacityT: 80 }])).toEqual({ usedT: 20, capacityT: 80, pct: 25 })
    })
  })

  describe('qualityBreakdown edge cases', () => {
    it('buckets empty quality as UNKNOWN and tolerates missing tons', () => {
      const rows = qualityBreakdown([
        { id: 'a', unitId: 'u', farmId: 'f', crop: 'SOY', tons: undefined as unknown as number, quality: '', receivedAt: '' },
        { id: 'b', unitId: 'u', farmId: 'f', crop: 'SOY', tons: 2, quality: 'A', receivedAt: '' },
      ])
      expect(rows).toEqual([
        { quality: 'A', count: 1, tons: 2 },
        { quality: 'UNKNOWN', count: 1, tons: 0 },
      ])
      expect(qualityBreakdown([])).toEqual([])
    })
  })

  describe('flowStages state derivation', () => {
    const lot = (id: string, quality: string) => ({ id, unitId: 'u', farmId: 'f', crop: 'SOY', tons: 1, quality, receivedAt: '' })
    const stateOf = (stages: ReturnType<typeof flowStages>, id: string) => stages.find((s) => s.id === id)

    it('harvest: done only when every plan is done, pending when mixed or nothing active', () => {
      expect(stateOf(flowStages([{ id: 'p1', status: 'COMPLETED' }, { id: 'p2', status: 'closed' }], [], []), 'harvest')?.state).toBe('done')
      expect(stateOf(flowStages([{ id: 'p1', status: 'COMPLETED' }, { id: 'p2', status: 'PLANNED' }], [], []), 'harvest')?.state).toBe('pending')
      expect(stateOf(flowStages([{ id: 'p1', status: 'PLANNED' }], [], []), 'harvest')).toEqual({ id: 'harvest', state: 'pending', count: 0 })
      expect(stateOf(flowStages([{ id: 'p1', status: 'active' }, { id: 'p2', status: 'RUNNING' }], [], []), 'harvest')?.count).toBe(2)
    })

    it('transport: now while in transit, done when only delivered, pending when only queued', () => {
      expect(stateOf(flowStages([], [{ id: 'l', status: 'DELIVERED' }], []), 'transport')).toEqual({ id: 'transport', state: 'done', count: 0 })
      expect(stateOf(flowStages([], [{ id: 'l', status: 'QUEUED' }], []), 'transport')?.state).toBe('pending')
      expect(stateOf(flowStages([], [{ id: 'l', status: 'DISPATCHED' }, { id: 'm', status: 'DELIVERED' }], []), 'transport')).toEqual({
        id: 'transport',
        state: 'now',
        count: 1,
      })
    })

    it('storage and quality: partially graded lots are "now", ungraded lots keep quality pending', () => {
      const mixed = flowStages([], [], [lot('a', 'A'), lot('b', 'UNKNOWN'), lot('c', '')])
      expect(stateOf(mixed, 'storage')).toEqual({ id: 'storage', state: 'done', count: 3 })
      expect(stateOf(mixed, 'quality')).toEqual({ id: 'quality', state: 'now', count: 1 })
      const ungraded = flowStages([], [], [lot('a', 'UNKNOWN')])
      expect(stateOf(ungraded, 'quality')).toEqual({ id: 'quality', state: 'pending', count: 0 })
    })
  })

  it('derives flow stage states', () => {
    const stages = flowStages(
      [{ id: 'p1', status: 'IN_PROGRESS' }],
      [{ id: 'l1', status: 'IN_TRANSIT', tons: 1 }],
      [{ id: 'a', unitId: 'u', farmId: 'f', crop: 'SOY', tons: 10, quality: 'A', receivedAt: '' }],
    )
    expect(stages.map((s) => `${s.id}:${s.state}`)).toEqual(['harvest:now', 'transport:now', 'storage:done', 'quality:done'])
    expect(flowStages([], [], []).every((s) => s.state === 'pending')).toBe(true)
  })
})
