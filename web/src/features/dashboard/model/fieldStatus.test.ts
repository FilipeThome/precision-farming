import { describe, expect, it } from 'vitest'

import { countByState, fieldStates } from '@/features/dashboard/model/fieldStatus'
import type { Field, Operation } from '@/shared/api/types'

const fields = [{ id: 'a' }, { id: 'b' }, { id: 'c' }] as Field[]

function op(
  fieldId: string,
  status: string,
  times?: { actualEnd?: string | null; plannedEnd?: string | null },
): Operation {
  return {
    id: `${fieldId}-${status}-${times?.actualEnd ?? times?.plannedEnd ?? 'na'}`,
    fieldId,
    status,
    actualEnd: times?.actualEnd ?? null,
    plannedEnd: times?.plannedEnd ?? null,
  } as Operation
}

describe('fieldStates', () => {
  it.each([
    ['in progress wins over planned and completed', [op('a', 'COMPLETED'), op('a', 'PLANNED'), op('a', 'IN_PROGRESS')], 'progress'],
    ['paused maps to blocked', [op('a', 'PAUSED'), op('a', 'PLANNED')], 'blocked'],
    ['planned wins over completed', [op('a', 'COMPLETED'), op('a', 'PLANNED')], 'planned'],
    ['completed only', [op('a', 'COMPLETED')], 'done'],
  ])('%s', (_name, ops, expected) => {
    expect(fieldStates(fields, ops)['a']).toBe(expected)
  })

  it('includes every field and ignores unknown fields', () => {
    const states = fieldStates(fields, [op('a', 'PLANNED'), op('zzz', 'IN_PROGRESS')])
    expect(states).toEqual({ a: 'planned', b: 'none', c: 'none' })
    expect(countByState(states).planned).toBe(1)
    expect(countByState(states).progress).toBe(0)
    expect(countByState(states).none).toBe(2)
  })

  it('follows the full precedence chain in progress > paused > planned > completed', () => {
    expect(fieldStates(fields, [op('a', 'PAUSED'), op('a', 'IN_PROGRESS')])['a']).toBe('progress')
    expect(fieldStates(fields, [op('a', 'COMPLETED'), op('a', 'PAUSED')])['a']).toBe('blocked')
    expect(fieldStates(fields, [op('a', 'COMPLETED'), op('a', 'IN_PROGRESS'), op('a', 'PAUSED'), op('a', 'PLANNED')])['a']).toBe(
      'progress',
    )
  })

  it('is independent of operation order', () => {
    const ops = [op('a', 'COMPLETED'), op('a', 'PAUSED'), op('a', 'PLANNED')]
    expect(fieldStates(fields, ops)).toEqual(fieldStates(fields, [...ops].reverse()))
  })

  it('ignores statuses outside the four mapped ones and matches status case-insensitively', () => {
    expect(fieldStates(fields, [op('a', 'CANCELLED')])).toEqual({ a: 'none', b: 'none', c: 'none' })
    expect(fieldStates(fields, [op('a', 'CANCELLED'), op('a', 'COMPLETED')])).toEqual({
      a: 'done',
      b: 'none',
      c: 'none',
    })
    expect(fieldStates(fields, [op('a', 'in_progress')])).toEqual({ a: 'progress', b: 'none', c: 'none' })
  })

  it('handles several fields and empty inputs', () => {
    expect(fieldStates(fields, [op('a', 'PLANNED'), op('b', 'IN_PROGRESS'), op('c', 'COMPLETED')])).toEqual({
      a: 'planned',
      b: 'progress',
      c: 'done',
    })
    expect(fieldStates([], [op('a', 'PLANNED')])).toEqual({})
    expect(fieldStates(fields, [])).toEqual({ a: 'none', b: 'none', c: 'none' })
  })

  it('marks a winning done field stale when the latest completion is older than 24h', () => {
    const now = Date.parse('2026-09-10T12:00:00.000Z')
    const staleAfter = 24 * 60 * 60 * 1000
    const older = new Date(now - staleAfter - 1).toISOString()
    const exactly = new Date(now - staleAfter).toISOString()
    const recent = new Date(now - 60 * 60 * 1000).toISOString()

    expect(fieldStates(fields, [op('a', 'COMPLETED', { actualEnd: older })], now).a).toBe('stale')
    expect(fieldStates(fields, [op('a', 'COMPLETED', { actualEnd: exactly })], now).a).toBe('done')
    expect(fieldStates(fields, [op('a', 'COMPLETED', { actualEnd: recent, plannedEnd: older })], now).a).toBe('done')
    expect(fieldStates(fields, [op('a', 'COMPLETED', { plannedEnd: older })], now).a).toBe('stale')
    expect(
      fieldStates(fields, [op('a', 'COMPLETED', { actualEnd: older }), op('a', 'COMPLETED', { actualEnd: recent })], now).a,
    ).toBe('done')
    expect(fieldStates(fields, [op('a', 'COMPLETED')], now).a).toBe('done')
    expect(fieldStates(fields, [op('a', 'COMPLETED', { actualEnd: older }), op('a', 'PLANNED')], now).a).toBe('planned')
    expect(fieldStates(fields, [op('a', 'completed', { actualEnd: older })], now).a).toBe('stale')
  })

  it('countByState returns every bucket, zeroed when empty', () => {
    expect(countByState({})).toEqual({ progress: 0, blocked: 0, planned: 0, done: 0, stale: 0, none: 0 })
    expect(countByState({ a: 'done', b: 'done', c: 'blocked' })).toMatchObject({ done: 2, blocked: 1, progress: 0 })
  })
})
