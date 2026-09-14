import { describe, expect, it } from 'vitest'

import { CHAIN_STEP_IDS, chainSteps, linkedOperation } from '@/features/decisions/chain'
import type { DecisionItem } from '@/features/decisions/model'
import type { Operation } from '@/shared/api/types'

const base: DecisionItem = {
  id: 'PRESCRIPTION:p1',
  rawId: 'p1',
  source: 'PRESCRIPTION',
  title: 'GLYPHOSATE',
  fieldId: 'field-1',
  farmId: 'farm-1',
  status: 'PENDING',
  createdAt: '2026-09-10T08:00:00Z',
  capabilities: { approve: true, simulate: false },
}

const op: Operation = {
  id: 'op-1',
  fieldId: 'field-1',
  farmId: 'farm-1',
  type: 'SPRAYING',
  status: 'IN_PROGRESS',
  plannedStart: '2026-09-10T10:00:00Z',
  plannedEnd: '2026-09-10T14:00:00Z',
  actualStart: '2026-09-10T10:05:00Z',
  actualEnd: null,
  machineId: null,
  pauseReason: null,
  itemId: null,
  itemQuantity: null,
  areaHa: null,
}

function states(item: DecisionItem, ops: Operation[] = []) {
  return Object.fromEntries(chainSteps(item, ops).map((s) => [s.id, s.state]))
}

describe('chainSteps', () => {
  it('pending decision: approval is the current step', () => {
    expect(states(base)).toEqual({
      signal: 'done',
      context: 'done',
      recommendation: 'done',
      approval: 'now',
      order: 'pending',
      execution: 'pending',
    })
  })

  it('missing createdAt makes the signal pending, not done', () => {
    expect(states({ ...base, createdAt: undefined, fieldId: undefined })).toMatchObject({
      signal: 'pending',
      context: 'pending',
    })
  })

  it('approved without a linked operation: order is now', () => {
    expect(states({ ...base, status: 'APPROVED', approvedAt: '2026-09-10T09:00:00Z' })).toMatchObject({
      approval: 'done',
      order: 'now',
      execution: 'pending',
    })
  })

  it('approved with an inferred linked operation in progress', () => {
    const steps = chainSteps({ ...base, status: 'APPROVED', approvedAt: '2026-09-10T09:00:00Z' }, [op])
    const byId = Object.fromEntries(steps.map((s) => [s.id, s]))
    expect(byId.order.state).toBe('done')
    expect(byId.order.inferred).toBe(true)
    expect(byId.execution.state).toBe('now')
    expect(chainSteps({ ...base, status: 'APPROVED', approvedAt: '2026-09-10T09:00:00Z' }, [{ ...op, status: 'COMPLETED' }])[5].state).toBe('done')
  })

  it('rejected decision blocks the approval step', () => {
    expect(states({ ...base, status: 'REJECTED' })).toMatchObject({ approval: 'blocked', order: 'pending' })
  })

  it('always returns the six steps in rail order', () => {
    expect(chainSteps(base).map((s) => s.id)).toEqual(['signal', 'context', 'recommendation', 'approval', 'order', 'execution'])
    expect(CHAIN_STEP_IDS).toEqual(['signal', 'context', 'recommendation', 'approval', 'order', 'execution'])
  })

  it('approved without approvedAt cannot infer an order: order is now, nothing inferred', () => {
    const steps = chainSteps({ ...base, status: 'APPROVED', approvedAt: undefined }, [op])
    const byId = Object.fromEntries(steps.map((s) => [s.id, s]))
    expect(byId.approval.state).toBe('done')
    expect(byId.order).toEqual({ id: 'order', state: 'now', inferred: false })
    expect(byId.execution).toEqual({ id: 'execution', state: 'pending', inferred: false })
  })

  it('executed decision without a linked operation marks order and execution done', () => {
    expect(states({ ...base, status: 'EXECUTED' })).toMatchObject({ approval: 'done', order: 'done', execution: 'done' })
  })

  it('a linked operation overrides the EXECUTED shortcut with its real status', () => {
    const executed = { ...base, status: 'EXECUTED' as const, approvedAt: '2026-09-10T09:00:00Z' }
    expect(states(executed, [op])).toMatchObject({ order: 'done', execution: 'now' })
    expect(states(executed, [{ ...op, status: 'PAUSED' }])).toMatchObject({ execution: 'now' })
    expect(states(executed, [{ ...op, status: 'PLANNED', actualStart: null }])).toMatchObject({ order: 'done', execution: 'pending' })
  })

  it('unknown status leaves approval, order and execution pending', () => {
    expect(states({ ...base, status: 'UNKNOWN' })).toMatchObject({ approval: 'pending', order: 'pending', execution: 'pending' })
  })

  it('rejected decisions never show an order even when an operation would match', () => {
    const rejected = { ...base, status: 'REJECTED' as const, approvedAt: '2026-09-10T09:00:00Z' }
    expect(states(rejected, [op])).toMatchObject({ approval: 'blocked', order: 'pending', execution: 'pending' })
  })

  it('context is done from explanation lines or a summary when no field is resolved', () => {
    const noField = { ...base, fieldId: undefined }
    expect(states(noField).context).toBe('pending')
    expect(states({ ...noField, explanation: ['NDVI_DROP'] }).context).toBe('done')
    expect(states({ ...noField, explanation: [] }).context).toBe('pending')
    expect(states({ ...noField, summary: 'Low soil moisture' }).context).toBe('done')
  })

  it('does not flag steps as inferred when no operation is linked', () => {
    expect(chainSteps(base).filter((s) => s.inferred)).toEqual([])
  })
})

describe('linkedOperation', () => {
  it('ignores operations that started before the approval or on other fields', () => {
    const approved = { ...base, status: 'APPROVED' as const, approvedAt: '2026-09-10T12:00:00Z' }
    expect(linkedOperation(approved, [op])).toBeUndefined()
    expect(linkedOperation({ ...approved, approvedAt: '2026-09-10T09:00:00Z' }, [{ ...op, fieldId: 'other' }])).toBeUndefined()
    expect(linkedOperation({ ...approved, approvedAt: '2026-09-10T09:00:00Z' }, [op])?.id).toBe('op-1')
  })

  it('requires approvedAt', () => {
    expect(linkedOperation(base, [op])).toBeUndefined()
  })

  it('requires a resolved fieldId and a parseable approvedAt', () => {
    const approved = { ...base, status: 'APPROVED' as const, approvedAt: '2026-09-10T09:00:00Z' }
    expect(linkedOperation({ ...approved, fieldId: undefined }, [op])).toBeUndefined()
    expect(linkedOperation({ ...approved, approvedAt: 'not-a-date' }, [op])).toBeUndefined()
  })

  it('includes an operation that starts exactly at the approval instant', () => {
    const approved = { ...base, status: 'APPROVED' as const, approvedAt: '2026-09-10T10:05:00Z' }
    expect(linkedOperation(approved, [op])?.id).toBe('op-1')
  })

  it('falls back to plannedStart when actualStart is missing and skips undated operations', () => {
    const approved = { ...base, status: 'APPROVED' as const, approvedAt: '2026-09-10T09:00:00Z' }
    const planned = { ...op, id: 'planned', status: 'PLANNED', actualStart: null }
    expect(linkedOperation(approved, [planned])?.id).toBe('planned')
    const undated = { ...op, id: 'undated', actualStart: null, plannedStart: null }
    expect(linkedOperation(approved, [undated])).toBeUndefined()
    const badDate = { ...op, id: 'bad', actualStart: 'garbage', plannedStart: null }
    expect(linkedOperation(approved, [badDate])).toBeUndefined()
  })

  it('links only when exactly one operation matches after approval', () => {
    const approved = { ...base, status: 'APPROVED' as const, approvedAt: '2026-09-10T09:00:00Z' }
    const later = { ...op, id: 'later', actualStart: '2026-09-10T12:00:00Z' }
    const earlier = { ...op, id: 'earlier', actualStart: '2026-09-10T09:30:00Z' }
    const before = { ...op, id: 'before', actualStart: '2026-09-10T08:00:00Z' }
    expect(linkedOperation(approved, [later, before, earlier])).toBeUndefined()
    expect(linkedOperation(approved, [earlier, later])).toBeUndefined()
    expect(linkedOperation(approved, [earlier, before])?.id).toBe('earlier')
  })

  it('returns undefined for an empty operation list', () => {
    const approved = { ...base, status: 'APPROVED' as const, approvedAt: '2026-09-10T09:00:00Z' }
    expect(linkedOperation(approved, [])).toBeUndefined()
  })
})
