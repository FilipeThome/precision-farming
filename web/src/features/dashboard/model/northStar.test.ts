import { describe, expect, it } from 'vitest'

import {
  favorableWindowUntil,
  fleetAvailability,
  openCriticalAlerts,
  ratio,
  traceabilityRatio,
  withinWindowRatio,
} from '@/features/dashboard/model/northStar'
import type { Alert, Machine, Operation, WeatherWindow } from '@/shared/api/types'

function op(over: Partial<Operation>): Operation {
  return {
    id: 'op',
    fieldId: 'f',
    farmId: 'farm',
    type: 'SPRAYING',
    status: 'COMPLETED',
    plannedStart: null,
    plannedEnd: null,
    actualStart: null,
    actualEnd: null,
    machineId: null,
    pauseReason: null,
    itemId: null,
    itemQuantity: null,
    areaHa: null,
    ...over,
  }
}

describe('withinWindowRatio', () => {
  it.each([
    ['no operations', [], { numerator: 0, denominator: 0, pct: null }],
    ['completed without dates are not measurable', [op({})], { numerator: 0, denominator: 0, pct: null }],
    [
      'on time vs late',
      [
        op({ id: 'a', plannedEnd: '2026-09-10T12:00:00Z', actualEnd: '2026-09-10T11:30:00Z' }),
        op({ id: 'b', plannedEnd: '2026-09-10T12:00:00Z', actualEnd: '2026-09-10T13:00:00Z' }),
        op({ id: 'c', status: 'IN_PROGRESS', plannedEnd: '2026-09-10T12:00:00Z', actualEnd: '2026-09-10T11:00:00Z' }),
      ],
      { numerator: 1, denominator: 2, pct: 50 },
    ],
  ])('%s', (_name, ops, expected) => {
    expect(withinWindowRatio(ops as Operation[])).toEqual(expected)
  })

  it('counts finishing exactly at plannedEnd as within the window', () => {
    expect(withinWindowRatio([op({ plannedEnd: '2026-09-10T12:00:00Z', actualEnd: '2026-09-10T12:00:00Z' })])).toEqual({
      numerator: 1,
      denominator: 1,
      pct: 100,
    })
  })

  it('ignores unparseable or half-missing dates', () => {
    const ops = [
      op({ id: 'bad', plannedEnd: 'garbage', actualEnd: '2026-09-10T11:00:00Z' }),
      op({ id: 'noPlanned', actualEnd: '2026-09-10T11:00:00Z' }),
      op({ id: 'noActual', plannedEnd: '2026-09-10T12:00:00Z' }),
    ]
    expect(withinWindowRatio(ops)).toEqual({ numerator: 0, denominator: 0, pct: null })
  })

  it('all late is 0%, not "—"', () => {
    expect(withinWindowRatio([op({ plannedEnd: '2026-09-10T12:00:00Z', actualEnd: '2026-09-10T13:00:00Z' })]).pct).toBe(0)
  })
})

describe('ratio', () => {
  it.each([
    [0, 0, null],
    [0, 5, 0],
    [1, 3, 33],
    [2, 3, 67],
    [1, 2, 50],
    [5, 5, 100],
  ])('%i/%i → %s', (n, d, pct) => {
    expect(ratio(n, d)).toEqual({ numerator: n, denominator: d, pct })
  })
})

describe('secondary KPIs', () => {
  it('traceability counts ops with machine and item', () => {
    expect(traceabilityRatio([op({ machineId: 'm', itemId: 'i' }), op({ machineId: 'm' }), op({})])).toEqual({
      numerator: 1,
      denominator: 3,
      pct: 33,
    })
    expect(traceabilityRatio([]).pct).toBeNull()
  })

  it('counts open critical alerts only', () => {
    const alerts = [
      { status: 'OPEN', severity: 'CRITICAL' },
      { status: 'ACKED', severity: 'CRITICAL' },
      { status: 'OPEN', severity: 'WARNING' },
    ] as Alert[]
    expect(openCriticalAlerts(alerts)).toBe(1)
  })

  it('fleet availability = operating + idle / total', () => {
    const machines = [{ status: 'OPERATING' }, { status: 'IDLE' }, { status: 'MAINTENANCE' }] as Machine[]
    expect(fleetAvailability(machines)).toEqual({ numerator: 2, denominator: 3, pct: 67 })
  })

  it('fleet availability with no machines is "—", all unavailable is 0%', () => {
    expect(fleetAvailability([])).toEqual({ numerator: 0, denominator: 0, pct: null })
    expect(fleetAvailability([{ status: 'MAINTENANCE' }, { status: 'BROKEN' }] as Machine[]).pct).toBe(0)
  })

  it('traceability treats empty ids as unlinked', () => {
    expect(traceabilityRatio([op({ machineId: '', itemId: 'i' }), op({ machineId: 'm', itemId: '' })]).numerator).toBe(0)
  })

  it('openCriticalAlerts is 0 for an empty list', () => {
    expect(openCriticalAlerts([])).toBe(0)
  })
})

describe('favorableWindowUntil', () => {
  const now = Date.parse('2026-09-10T10:00:00Z')
  const window = (over: Partial<WeatherWindow>): WeatherWindow => ({
    id: 'w',
    farmId: 'farm',
    windowType: 'SPRAY',
    startAt: '2026-09-10T08:00:00Z',
    endAt: '2026-09-10T11:30:00Z',
    rating: 'FAVORABLE',
    notes: null,
    ...over,
  })

  it('returns the end of a favorable window covering now', () => {
    expect(favorableWindowUntil([window({})], now)).toBe('2026-09-10T11:30:00Z')
  })

  it('ignores marginal or non-covering windows', () => {
    expect(favorableWindowUntil([window({ rating: 'MARGINAL' })], now)).toBeNull()
    expect(favorableWindowUntil([window({ startAt: '2026-09-10T12:00:00Z', endAt: '2026-09-10T14:00:00Z' })], now)).toBeNull()
    expect(favorableWindowUntil([], now)).toBeNull()
  })

  it('window bounds are [startAt, endAt): covered at start, not at end', () => {
    expect(favorableWindowUntil([window({ startAt: '2026-09-10T10:00:00Z' })], now)).toBe('2026-09-10T11:30:00Z')
    expect(favorableWindowUntil([window({ endAt: '2026-09-10T10:00:00Z' })], now)).toBeNull()
  })

  it('returns the latest end when several favorable windows cover now', () => {
    const windows = [
      window({ id: 'short', endAt: '2026-09-10T10:30:00Z' }),
      window({ id: 'long', endAt: '2026-09-10T16:00:00Z' }),
      window({ id: 'mid', endAt: '2026-09-10T12:00:00Z' }),
    ]
    expect(favorableWindowUntil(windows, now)).toBe('2026-09-10T16:00:00Z')
  })

  it('skips windows with unparseable dates', () => {
    expect(favorableWindowUntil([window({ startAt: 'nope' }), window({ endAt: '' })], now)).toBeNull()
  })
})
