import type { Field, Operation } from '@/shared/api/types'
import type { FieldState } from '@/shared/maps/fieldStateColors'

const RANK: Record<FieldState, number> = { progress: 0, blocked: 1, planned: 2, done: 3, stale: 4, none: 5 }

const STALE_AFTER_MS = 24 * 60 * 60 * 1000

function stateOf(status: string): FieldState | null {
  switch (status.toUpperCase()) {
    case 'IN_PROGRESS':
      return 'progress'
    case 'PAUSED':
      return 'blocked'
    case 'PLANNED':
      return 'planned'
    case 'COMPLETED':
      return 'done'
    default:
      return null
  }
}

function parseEpoch(value: string | null | undefined): number | null {
  if (!value || value.trim() === '') return null
  const at = Date.parse(value)
  return Number.isNaN(at) ? null : at
}

function isStaleDone(fieldId: string, operations: Operation[], now: number, staleAfterMs: number): boolean {
  const ends = operations
    .filter((op) => op.fieldId === fieldId && op.status.toUpperCase() === 'COMPLETED')
    .map((op) => parseEpoch(op.actualEnd) ?? parseEpoch(op.plannedEnd))
    .filter((at): at is number => at != null)
  const last = ends.length === 0 ? null : Math.max(...ends)
  if (last == null) return false
  return now - last > staleAfterMs
}

/**
 * Field → operation state used for Tower / Map status lists.
 * Priority: in progress > paused > planned > completed.
 * Fields without operations are `none`.
 * A winning `done` whose latest COMPLETED actualEnd (else plannedEnd) is older than 24h is `stale`.
 */
export function fieldStates(
  fields: Field[],
  operations: Operation[],
  now: number = Date.now(),
  staleAfterMs: number = STALE_AFTER_MS,
): Record<string, FieldState> {
  const known = new Set(fields.map((field) => field.id))
  const result: Record<string, FieldState> = {}
  for (const op of operations) {
    if (!op.fieldId || !known.has(op.fieldId)) continue
    const next = stateOf(op.status)
    if (!next) continue
    const current = result[op.fieldId]
    if (!current || RANK[next] < RANK[current]) result[op.fieldId] = next
  }
  for (const field of fields) {
    const current = result[field.id]
    if (!current) {
      result[field.id] = 'none'
      continue
    }
    if (current === 'done' && isStaleDone(field.id, operations, now, staleAfterMs)) {
      result[field.id] = 'stale'
    }
  }
  return result
}

export function countByState(states: Record<string, FieldState>): Record<FieldState, number> {
  const counts: Record<FieldState, number> = { progress: 0, blocked: 0, planned: 0, done: 0, stale: 0, none: 0 }
  for (const state of Object.values(states)) counts[state] += 1
  return counts
}
