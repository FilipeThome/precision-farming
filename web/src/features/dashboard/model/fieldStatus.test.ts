import { describe, expect, it } from 'vitest'

import { countByState, fieldStates } from '@/features/dashboard/model/fieldStatus'
import type { Field, Operation } from '@/shared/api/types'

const fields = [{ id: 'a' }, { id: 'b' }, { id: 'c' }] as Field[]

function op(fieldId: string, status: string): Operation {
  return { id: `${fieldId}-${status}`, fieldId, status } as Operation
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

  it('omits fields without operations and ignores unknown fields', () => {
    const states = fieldStates(fields, [op('a', 'PLANNED'), op('zzz', 'IN_PROGRESS')])
    expect(states).toEqual({ a: 'planned' })
    expect(countByState(states).planned).toBe(1)
    expect(countByState(states).progress).toBe(0)
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

  it('ignores statuses outside the four mapped ones', () => {
    expect(fieldStates(fields, [op('a', 'CANCELLED')])).toEqual({})
    expect(fieldStates(fields, [op('a', 'CANCELLED'), op('a', 'COMPLETED')])).toEqual({ a: 'done' })
    expect(fieldStates(fields, [op('a', 'in_progress')])).toEqual({})
  })

  it('handles several fields and empty inputs', () => {
    expect(fieldStates(fields, [op('a', 'PLANNED'), op('b', 'IN_PROGRESS'), op('c', 'COMPLETED')])).toEqual({
      a: 'planned',
      b: 'progress',
      c: 'done',
    })
    expect(fieldStates([], [op('a', 'PLANNED')])).toEqual({})
    expect(fieldStates(fields, [])).toEqual({})
  })

  it('countByState returns every bucket, zeroed when empty', () => {
    expect(countByState({})).toEqual({ progress: 0, blocked: 0, planned: 0, done: 0, stale: 0, none: 0 })
    expect(countByState({ a: 'done', b: 'done', c: 'blocked' })).toMatchObject({ done: 2, blocked: 1, progress: 0 })
  })
})
