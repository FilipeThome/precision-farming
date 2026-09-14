import { needsHumanReview, type DecisionItem, type DecisionStatus } from './model'

const PRIORITY_RANK: Record<string, number> = { CRITICAL: 0, HIGH: 1, MEDIUM: 2, LOW: 3 }

function priorityRank(item: DecisionItem): number {
  const key = item.priority?.toUpperCase()
  return key && key in PRIORITY_RANK ? PRIORITY_RANK[key] : 4
}

function statusRank(status: DecisionStatus): number {
  if (status === 'PENDING') return 0
  if (status === 'UNKNOWN') return 1
  if (status === 'APPROVED') return 2
  if (status === 'EXECUTED') return 3
  return 4
}

/** Decisions that still need a human: pending approvals. Shared by the TrustStrip and the Decisions page. */
export function pendingDecisions(items: DecisionItem[]): DecisionItem[] {
  return items.filter((item) => item.status === 'PENDING')
}

/** Items that require attention in the action queue: pending approvals + low-confidence insights. */
export function actionableDecisions(items: DecisionItem[]): DecisionItem[] {
  return items.filter((item) => item.status === 'PENDING' || needsHumanReview(item))
}

/** Severity/priority first, then pending status, then low confidence, then newest first. */
export function sortByPriority(items: DecisionItem[]): DecisionItem[] {
  return [...items].sort((a, b) => {
    const p = priorityRank(a) - priorityRank(b)
    if (p !== 0) return p
    const s = statusRank(a.status) - statusRank(b.status)
    if (s !== 0) return s
    const review = Number(needsHumanReview(b)) - Number(needsHumanReview(a))
    if (review !== 0) return review
    return (b.createdAt ?? '').localeCompare(a.createdAt ?? '')
  })
}

export type DecisionFilter = 'pending' | 'approved' | 'all'

export function filterDecisions(items: DecisionItem[], filter: DecisionFilter): DecisionItem[] {
  if (filter === 'pending') return pendingDecisions(items)
  if (filter === 'approved') return items.filter((i) => i.status === 'APPROVED' || i.status === 'EXECUTED')
  return items
}
