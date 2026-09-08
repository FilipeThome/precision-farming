import type { HarvestPlan, YieldRecord } from '@/shared/api/types'

export function weightedYieldTHa(
  rows: Array<{ yieldTHa?: number | null; areaHa?: number | null }>,
): number {
  const usable = rows.filter((row) => row.yieldTHa != null && Number.isFinite(Number(row.yieldTHa)))
  if (usable.length === 0) return 0
  const withArea = usable.filter((row) => row.areaHa != null && Number(row.areaHa) > 0)
  if (withArea.length > 0) {
    const num = withArea.reduce((sum, row) => sum + Number(row.yieldTHa) * Number(row.areaHa), 0)
    const den = withArea.reduce((sum, row) => sum + Number(row.areaHa), 0)
    return den === 0 ? 0 : num / den
  }
  return usable.reduce((sum, row) => sum + Number(row.yieldTHa), 0) / usable.length
}

export function yieldsForPlan(rows: YieldRecord[], plan: HarvestPlan): YieldRecord[] {
  return rows.filter(
    (row) => row.planId === plan.id || (row.planId == null && row.fieldId === plan.fieldId),
  )
}

export function yieldsForField(rows: YieldRecord[], fieldId: string): YieldRecord[] {
  return rows.filter((row) => row.fieldId === fieldId)
}
