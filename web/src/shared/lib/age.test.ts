import { describe, expect, it } from 'vitest'

import { ageFromMs, ageMs, ageOf, DEFAULT_STALE_MS, formatAge } from '@/shared/lib/age'

const NOW = Date.parse('2026-09-10T12:00:00Z')

describe('age', () => {
  it.each([
    [45_000, '45s'],
    [4 * 60_000, '4 min'],
    [2 * 3_600_000, '2h'],
    [36 * 3_600_000, '36h'],
    [12 * 86_400_000, '12d'],
  ])('formats %i ms as %s', (ms, expected) => {
    expect(formatAge(ageFromMs(ms))).toBe(expected)
  })

  it('computes age relative to now', () => {
    expect(formatAge(ageOf('2026-09-10T10:00:00Z', NOW)!)).toBe('2h')
    expect(ageMs('2026-09-10T11:56:00Z', NOW)).toBe(4 * 60_000)
  })

  it('returns null for missing or invalid timestamps', () => {
    expect(ageOf(null, NOW)).toBeNull()
    expect(ageOf('', NOW)).toBeNull()
    expect(ageOf('not-a-date', NOW)).toBeNull()
    expect(ageMs(undefined, NOW)).toBeNull()
  })

  it('clamps future timestamps to zero', () => {
    expect(ageMs('2026-09-11T00:00:00Z', NOW)).toBe(0)
    expect(formatAge(ageOf('2026-09-11T00:00:00Z', NOW)!)).toBe('0s')
  })

  it.each([
    [0, { value: 0, unit: 's' }],
    [59_999, { value: 59, unit: 's' }],
    [60_000, { value: 1, unit: 'min' }],
    [3_599_999, { value: 59, unit: 'min' }],
    [3_600_000, { value: 1, unit: 'h' }],
    [24 * 3_600_000, { value: 24, unit: 'h' }],
    [3 * 86_400_000 - 1, { value: 71, unit: 'h' }],
    [3 * 86_400_000, { value: 3, unit: 'd' }],
    [-5_000, { value: 0, unit: 's' }],
  ])('bucket boundary %i ms → %o', (ms, expected) => {
    expect(ageFromMs(ms)).toEqual(expected)
  })

  it('accepts Date and epoch-number inputs', () => {
    expect(ageMs(new Date(NOW - 90_000), NOW)).toBe(90_000)
    expect(ageMs(NOW - 90_000, NOW)).toBe(90_000)
    expect(formatAge(ageOf(new Date(NOW - 90_000), NOW)!)).toBe('1 min')
  })

  it('exposes a 24h default stale threshold', () => {
    expect(DEFAULT_STALE_MS).toBe(24 * 3_600_000)
    expect(ageMs('2026-09-09T12:00:00Z', NOW)).toBe(DEFAULT_STALE_MS)
    expect(ageMs('2026-09-09T11:59:59Z', NOW)! > DEFAULT_STALE_MS).toBe(true)
  })
})
