import { describe, expect, it } from 'vitest'

import { timelineRows, todayOperations, todayRange, toSegment, windowBands } from '@/features/dashboard/model/todayTimeline'
import type { Operation, WeatherWindow } from '@/shared/api/types'

const day = { start: Date.parse('2026-09-10T00:00:00Z'), end: Date.parse('2026-09-11T00:00:00Z') }

function op(over: Partial<Operation>): Operation {
  return {
    id: 'op',
    fieldId: 'f',
    farmId: 'farm',
    type: 'SPRAYING',
    status: 'PLANNED',
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

describe('todayRange', () => {
  it('covers the local calendar day', () => {
    const now = new Date(2026, 8, 10, 15, 30)
    const range = todayRange(now)
    expect(new Date(range.start).getHours()).toBe(0)
    expect(range.end - range.start).toBe(24 * 3_600_000)
  })

  it('is stable across the whole local day and flips exactly at local midnight', () => {
    const first = todayRange(new Date(2026, 8, 10, 0, 0, 0, 0))
    const last = todayRange(new Date(2026, 8, 10, 23, 59, 59, 999))
    expect(first).toEqual(last)
    expect(first.start).toBe(new Date(2026, 8, 10, 0, 0, 0, 0).getTime())
    const nextDay = todayRange(new Date(2026, 8, 11, 0, 0, 0, 0))
    expect(nextDay.start).toBe(first.end)
  })
})

describe('toSegment', () => {
  it.each([
    ['06h–12h', '2026-09-10T06:00:00Z', '2026-09-10T12:00:00Z', { leftPct: 25, widthPct: 25 }],
    ['clamps to the day', '2026-09-09T20:00:00Z', '2026-09-10T06:00:00Z', { leftPct: 0, widthPct: 25 }],
    ['open end runs to midnight', '2026-09-10T18:00:00Z', null, { leftPct: 75, widthPct: 25 }],
  ])('%s', (_name, start, end, expected) => {
    expect(toSegment(Date.parse(start), end ? Date.parse(end) : null, day)).toEqual(expected)
  })

  it('returns undefined when nothing falls in the range', () => {
    expect(toSegment(Date.parse('2026-09-12T00:00:00Z'), Date.parse('2026-09-12T02:00:00Z'), day)).toBeUndefined()
    expect(toSegment(null, null, day)).toBeUndefined()
  })

  it('handles degenerate and boundary inputs', () => {
    const t = (s: string) => Date.parse(s)
    // zero-length and inverted intervals render nothing
    expect(toSegment(t('2026-09-10T06:00:00Z'), t('2026-09-10T06:00:00Z'), day)).toBeUndefined()
    expect(toSegment(t('2026-09-10T12:00:00Z'), t('2026-09-10T06:00:00Z'), day)).toBeUndefined()
    // ending exactly at day start / starting exactly at day end → outside
    expect(toSegment(t('2026-09-09T20:00:00Z'), t('2026-09-10T00:00:00Z'), day)).toBeUndefined()
    expect(toSegment(t('2026-09-11T00:00:00Z'), t('2026-09-11T02:00:00Z'), day)).toBeUndefined()
    // whole day and beyond both ends clamps to 0..100
    expect(toSegment(t('2026-09-09T00:00:00Z'), t('2026-09-12T00:00:00Z'), day)).toEqual({ leftPct: 0, widthPct: 100 })
    // end-only interval starts at day start
    expect(toSegment(null, t('2026-09-10T06:00:00Z'), day)).toEqual({ leftPct: 0, widthPct: 25 })
    // 1 minute rounds to two decimals (1/1440 ≈ 0.0694%)
    expect(toSegment(t('2026-09-10T00:00:00Z'), t('2026-09-10T00:01:00Z'), day)).toEqual({ leftPct: 0, widthPct: 0.07 })
  })
})

describe('todayOperations / timelineRows', () => {
  const today = op({ id: 'today', plannedStart: '2026-09-10T08:00:00Z', plannedEnd: '2026-09-10T12:00:00Z' })
  const tomorrow = op({ id: 'tomorrow', plannedStart: '2026-09-11T08:00:00Z', plannedEnd: '2026-09-11T12:00:00Z' })
  const executed = op({
    id: 'exec',
    status: 'COMPLETED',
    plannedStart: '2026-09-10T06:00:00Z',
    plannedEnd: '2026-09-10T12:00:00Z',
    actualStart: '2026-09-10T06:00:00Z',
    actualEnd: '2026-09-10T09:00:00Z',
  })

  it('keeps only operations intersecting the day, sorted by start', () => {
    expect(todayOperations([tomorrow, today, executed], day).map((o) => o.id)).toEqual(['exec', 'today'])
  })

  it('builds planned and executed segments', () => {
    const rows = timelineRows([executed], day)
    expect(rows[0].planned).toEqual({ leftPct: 25, widthPct: 25 })
    expect(rows[0].executed).toEqual({ leftPct: 25, widthPct: 12.5 })
    expect(timelineRows([today], day)[0].executed).toBeUndefined()
  })

  it('is empty when nothing is planned today', () => {
    expect(timelineRows([tomorrow], day)).toEqual([])
    expect(timelineRows([], day)).toEqual([])
  })

  describe('window intersection across midnight', () => {
    const overnightIn = op({ id: 'in', plannedStart: '2026-09-09T22:00:00Z', plannedEnd: '2026-09-10T02:00:00Z' })
    const overnightOut = op({ id: 'out', plannedStart: '2026-09-10T22:00:00Z', plannedEnd: '2026-09-11T02:00:00Z' })
    const endsAtMidnight = op({ id: 'endsAtMidnight', plannedStart: '2026-09-09T20:00:00Z', plannedEnd: '2026-09-10T00:00:00Z' })
    const startsAtMidnight = op({ id: 'startsAtMidnight', plannedStart: '2026-09-11T00:00:00Z', plannedEnd: '2026-09-11T02:00:00Z' })
    const spansDays = op({ id: 'spans', plannedStart: '2026-09-08T00:00:00Z', plannedEnd: '2026-09-13T00:00:00Z' })

    it('keeps windows crossing either midnight, drops those touching only the boundary', () => {
      const ids = todayOperations([startsAtMidnight, overnightOut, endsAtMidnight, overnightIn, spansDays], day).map((o) => o.id)
      expect(ids).toEqual(['spans', 'in', 'out'])
    })

    it('clamps the planned bars of overnight windows to the day edges', () => {
      const rows = timelineRows([overnightIn, overnightOut], day)
      expect(rows[0].planned).toEqual({ leftPct: 0, widthPct: 8.33 })
      expect(rows[1].planned).toEqual({ leftPct: 91.67, widthPct: 8.33 })
    })
  })

  describe('partial timestamps', () => {
    it('uses the single known planned bound as a point window', () => {
      expect(todayOperations([op({ id: 'startOnly', plannedStart: '2026-09-10T14:00:00Z' })], day)).toHaveLength(1)
      expect(todayOperations([op({ id: 'endOnly', plannedEnd: '2026-09-10T14:00:00Z' })], day)).toHaveLength(1)
      expect(todayOperations([op({ id: 'startAtEnd', plannedStart: '2026-09-11T00:00:00Z' })], day)).toHaveLength(0)
      expect(todayOperations([op({ id: 'endAtStart', plannedEnd: '2026-09-10T00:00:00Z' })], day)).toHaveLength(0)
    })

    it('includes unplanned work that actually ran today and ignores undated or garbage operations', () => {
      const adHoc = op({ id: 'adhoc', status: 'IN_PROGRESS', actualStart: '2026-09-10T07:00:00Z' })
      const undated = op({ id: 'undated' })
      const garbage = op({ id: 'garbage', plannedStart: 'not-a-date', plannedEnd: 'nope' })
      expect(todayOperations([undated, adHoc, garbage], day).map((o) => o.id)).toEqual(['adhoc'])
    })

    it('sorts by plannedStart, falling back to actualStart', () => {
      const a = op({ id: 'a', plannedStart: '2026-09-10T10:00:00Z', plannedEnd: '2026-09-10T11:00:00Z' })
      const b = op({ id: 'b', actualStart: '2026-09-10T08:00:00Z' })
      const c = op({ id: 'c', plannedStart: '2026-09-10T09:00:00Z', plannedEnd: '2026-09-10T12:00:00Z' })
      expect(todayOperations([a, b, c], day).map((o) => o.id)).toEqual(['b', 'c', 'a'])
    })
  })

  describe('executed bar for ongoing operations', () => {
    const ongoing = op({
      id: 'ongoing',
      status: 'IN_PROGRESS',
      plannedStart: '2026-09-10T06:00:00Z',
      plannedEnd: '2026-09-10T12:00:00Z',
      actualStart: '2026-09-10T06:00:00Z',
    })

    it('runs from actualStart to now', () => {
      const [row] = timelineRows([ongoing], day, Date.parse('2026-09-10T09:00:00Z'))
      expect(row.executed).toEqual({ leftPct: 25, widthPct: 12.5 })
    })

    it('never extends past the end of the day', () => {
      const [row] = timelineRows([ongoing], day, Date.parse('2026-09-12T09:00:00Z'))
      expect(row.executed).toEqual({ leftPct: 25, widthPct: 75 })
    })

    it('is omitted when now is not after actualStart yet', () => {
      const [row] = timelineRows([ongoing], day, Date.parse('2026-09-10T05:00:00Z'))
      expect(row.executed).toBeUndefined()
      expect(row.planned).toBeDefined()
    })
  })
})

describe('windowBands', () => {
  it('maps weather windows to rated bands within the day', () => {
    const windows = [
      { id: 'w1', rating: 'FAVORABLE', startAt: '2026-09-10T00:00:00Z', endAt: '2026-09-10T12:00:00Z' },
      { id: 'w2', rating: 'UNFAVORABLE', startAt: '2026-09-12T00:00:00Z', endAt: '2026-09-12T12:00:00Z' },
    ] as WeatherWindow[]
    expect(windowBands(windows, day)).toEqual([{ rating: 'FAVORABLE', leftPct: 0, widthPct: 50 }])
  })

  it('clamps windows crossing midnight and preserves input order', () => {
    const windows = [
      { id: 'w1', rating: 'UNFAVORABLE', startAt: '2026-09-10T18:00:00Z', endAt: '2026-09-11T06:00:00Z' },
      { id: 'w2', rating: 'MARGINAL', startAt: '2026-09-09T18:00:00Z', endAt: '2026-09-10T06:00:00Z' },
      { id: 'w3', rating: 'FAVORABLE', startAt: 'garbage', endAt: '2026-09-10T06:00:00Z' },
    ] as WeatherWindow[]
    expect(windowBands(windows, day)).toEqual([
      { rating: 'UNFAVORABLE', leftPct: 75, widthPct: 25 },
      { rating: 'MARGINAL', leftPct: 0, widthPct: 25 },
      { rating: 'FAVORABLE', leftPct: 0, widthPct: 25 },
    ])
    expect(windowBands([], day)).toEqual([])
  })
})
