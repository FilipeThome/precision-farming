import type { Operation } from '@/shared/api/types'
import type { ChainStepState } from '@/shared/ui/ChainRail'

import type { DecisionItem } from './model'

export type ChainStepId = 'signal' | 'context' | 'recommendation' | 'approval' | 'order' | 'execution'

export type DerivedChainStep = {
  id: ChainStepId
  state: ChainStepState
  /** True when the state comes from a heuristic (e.g. linked operation by field + time). */
  inferred?: boolean
}

export const CHAIN_STEP_IDS: ChainStepId[] = ['signal', 'context', 'recommendation', 'approval', 'order', 'execution']

/**
 * Heuristic: an operation on the same field that started (or is planned to start) after the approval.
 * Labeled as inferred in the UI — the backend has no decision→operation link.
 */
export function linkedOperation(item: DecisionItem, operations: Operation[]): Operation | undefined {
  if (!item.fieldId || !item.approvedAt) return undefined
  const approved = Date.parse(item.approvedAt)
  if (Number.isNaN(approved)) return undefined
  const candidates = operations
    .filter((op) => op.fieldId === item.fieldId)
    .filter((op) => {
      const start = op.actualStart ?? op.plannedStart
      if (!start) return false
      const at = Date.parse(start)
      return !Number.isNaN(at) && at >= approved
    })
    .sort((a, b) => (a.actualStart ?? a.plannedStart ?? '').localeCompare(b.actualStart ?? b.plannedStart ?? ''))
  // Ambiguous matches are not linked — two orders on the same field must not share a chain.
  return candidates.length === 1 ? candidates[0] : undefined
}

/** Derives the six renderable chain steps for a decision from real data only. */
export function chainSteps(item: DecisionItem, operations: Operation[] = []): DerivedChainStep[] {
  const signal: ChainStepState = item.createdAt ? 'done' : 'pending'
  const hasContext = Boolean(item.fieldId) || (item.explanation?.length ?? 0) > 0 || Boolean(item.summary)
  const context: ChainStepState = hasContext ? 'done' : 'pending'
  const recommendation: ChainStepState = 'done'

  let approval: ChainStepState = 'pending'
  if (item.status === 'PENDING') approval = 'now'
  else if (item.status === 'APPROVED' || item.status === 'EXECUTED') approval = 'done'
  else if (item.status === 'REJECTED') approval = 'blocked'

  const op = linkedOperation(item, operations)
  const approvedLike = item.status === 'APPROVED' || item.status === 'EXECUTED'

  let order: ChainStepState = 'pending'
  let execution: ChainStepState = 'pending'
  if (item.status === 'REJECTED') {
    order = 'pending'
    execution = 'pending'
  } else if (op) {
    order = 'done'
    if (op.status === 'COMPLETED') execution = 'done'
    else if (op.status === 'IN_PROGRESS' || op.status === 'PAUSED') execution = 'now'
    else execution = 'pending'
  } else if (approvedLike) {
    order = item.status === 'EXECUTED' ? 'done' : 'now'
    execution = item.status === 'EXECUTED' ? 'done' : 'pending'
  }

  return [
    { id: 'signal', state: signal },
    { id: 'context', state: context },
    { id: 'recommendation', state: recommendation },
    { id: 'approval', state: approval },
    { id: 'order', state: order, inferred: Boolean(op) },
    { id: 'execution', state: execution, inferred: Boolean(op) },
  ]
}
