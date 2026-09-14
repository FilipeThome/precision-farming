import type { Field, Operation } from '@/shared/api/types'
import type { FieldState } from '@/shared/maps/fieldStateColors'

const RANK: Record<FieldState, number> = { progress: 0, blocked: 1, planned: 2, done: 3, stale: 4, none: 5 }

function stateOf(status: string): FieldState | null {
  if (status === 'IN_PROGRESS') return 'progress'
  if (status === 'PAUSED') return 'blocked'
  if (status === 'PLANNED') return 'planned'
  if (status === 'COMPLETED') return 'done'
  return null
}

/**
 * Field → operation state used to color the Tower map.
 * Priority: in progress > paused > planned > completed. Fields without operations are omitted (neutral).
 */
export function fieldStates(fields: Field[], operations: Operation[]): Record<string, FieldState> {
  const known = new Set(fields.map((f) => f.id))
  const result: Record<string, FieldState> = {}
  for (const op of operations) {
    if (!known.has(op.fieldId)) continue
    const next = stateOf(op.status)
    if (!next) continue
    const current = result[op.fieldId]
    if (!current || RANK[next] < RANK[current]) result[op.fieldId] = next
  }
  return result
}

export function countByState(states: Record<string, FieldState>): Record<FieldState, number> {
  const counts: Record<FieldState, number> = { progress: 0, blocked: 0, planned: 0, done: 0, stale: 0, none: 0 }
  for (const state of Object.values(states)) counts[state] += 1
  return counts
}
