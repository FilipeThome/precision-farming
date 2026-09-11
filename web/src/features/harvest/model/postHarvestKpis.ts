import type { Field, HarvestPlan, LogisticsLoad, StorageLot, StorageUnit, YieldRecord } from '@/shared/api/types'
import type { ChainStepState } from '@/shared/ui/ChainRail'

export type TonsCoverage = { tons: number; withArea: number; total: number }

/** Expected tons = Σ expectedTHa × field area, only for plans whose field has a known area. */
export function expectedTons(plans: HarvestPlan[], fields: Field[]): TonsCoverage {
  const areaById = new Map(fields.map((f) => [f.id, f.areaHa]))
  let tons = 0
  let withArea = 0
  for (const plan of plans) {
    const area = plan.fieldId ? areaById.get(plan.fieldId) : undefined
    if (plan.expectedTHa == null || area == null) continue
    tons += plan.expectedTHa * area
    withArea += 1
  }
  return { tons, withArea, total: plans.length }
}

/** Harvested tons = Σ yieldTHa × record area, only for records carrying an area. */
export function harvestedTons(records: YieldRecord[]): TonsCoverage {
  let tons = 0
  let withArea = 0
  for (const r of records) {
    if (r.yieldTHa == null || r.areaHa == null) continue
    tons += r.yieldTHa * r.areaHa
    withArea += 1
  }
  return { tons, withArea, total: records.length }
}

export type LoadsSummary = { queued: number; inTransit: number; delivered: number; total: number; tonsInTransit: number }

const IN_TRANSIT = new Set(['DISPATCHED', 'IN_TRANSIT', 'IN_PROGRESS'])
const DELIVERED = new Set(['DELIVERED', 'COMPLETED', 'RECEIVED'])

export function loadsSummary(loads: LogisticsLoad[]): LoadsSummary {
  const out: LoadsSummary = { queued: 0, inTransit: 0, delivered: 0, total: loads.length, tonsInTransit: 0 }
  for (const load of loads) {
    const status = (load.status ?? '').toUpperCase()
    if (status === 'QUEUED' || status === 'PLANNED') out.queued += 1
    else if (IN_TRANSIT.has(status)) {
      out.inTransit += 1
      out.tonsInTransit += load.tons ?? 0
    } else if (DELIVERED.has(status)) out.delivered += 1
  }
  return out
}

export type Occupancy = { usedT: number; capacityT: number; pct: number | null }

export function storageOccupancyTotal(units: StorageUnit[]): Occupancy {
  let usedT = 0
  let capacityT = 0
  for (const u of units) {
    usedT += u.usedT ?? 0
    capacityT += u.capacityT ?? 0
  }
  return { usedT, capacityT, pct: capacityT > 0 ? Math.round((usedT / capacityT) * 100) : null }
}

export function unitOccupancyPct(unit: StorageUnit): number | null {
  if (!unit.capacityT || unit.capacityT <= 0) return null
  return Math.min(100, Math.round(((unit.usedT ?? 0) / unit.capacityT) * 100))
}

export function qualityBreakdown(lots: StorageLot[]): Array<{ quality: string; count: number; tons: number }> {
  const map = new Map<string, { count: number; tons: number }>()
  for (const lot of lots) {
    const key = lot.quality || 'UNKNOWN'
    const cur = map.get(key) ?? { count: 0, tons: 0 }
    cur.count += 1
    cur.tons += lot.tons ?? 0
    map.set(key, cur)
  }
  return [...map.entries()].map(([quality, v]) => ({ quality, ...v })).sort((a, b) => b.tons - a.tons)
}

export type FlowStageId = 'harvest' | 'transport' | 'storage' | 'quality'
export type FlowStage = { id: FlowStageId; state: ChainStepState; count: number }

const ACTIVE_PLAN = new Set(['IN_PROGRESS', 'ACTIVE', 'RUNNING'])
const DONE_PLAN = new Set(['COMPLETED', 'DONE', 'CLOSED'])

export function flowStages(plans: HarvestPlan[], loads: LogisticsLoad[], lots: StorageLot[]): FlowStage[] {
  const active = plans.filter((p) => ACTIVE_PLAN.has((p.status ?? '').toUpperCase())).length
  const done = plans.filter((p) => DONE_PLAN.has((p.status ?? '').toUpperCase())).length
  const harvestState: ChainStepState =
    active > 0 ? 'now' : plans.length > 0 && done === plans.length ? 'done' : plans.length > 0 ? 'pending' : 'pending'

  const summary = loadsSummary(loads)
  const transportState: ChainStepState =
    summary.inTransit > 0 ? 'now' : summary.delivered > 0 ? 'done' : summary.queued > 0 ? 'pending' : 'pending'

  const storageState: ChainStepState = lots.length > 0 ? 'done' : 'pending'
  const graded = lots.filter((lot) => lot.quality && lot.quality !== 'UNKNOWN').length
  const qualityState: ChainStepState = graded === 0 ? 'pending' : graded === lots.length ? 'done' : 'now'

  return [
    { id: 'harvest', state: harvestState, count: active },
    { id: 'transport', state: transportState, count: summary.inTransit },
    { id: 'storage', state: storageState, count: lots.length },
    { id: 'quality', state: qualityState, count: graded },
  ]
}
