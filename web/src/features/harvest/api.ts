import { apiGet, apiPatch, apiPost } from '@/shared/api/client'
import type {
  HarvestPlan,
  LogisticsLoad,
  StorageLot,
  StorageUnit,
  YieldRecord,
} from '@/features/harvest/types'

export type HarvestPlanBody = {
  farmId: string
  fieldId: string
  crop: string
  expectedTHa: number
  plannedStart: string | null
  plannedEnd: string | null
}

export type StorageUnitBody = {
  farmId: string
  name: string
  type: string
  capacityT: number
  usedT: number
}

export async function fetchHarvestPlans(farmId?: string | null): Promise<HarvestPlan[]> {
  return apiGet<HarvestPlan[]>('/api/v1/harvest/plans', { farmId: farmId ?? undefined })
}

export async function createHarvestPlan(body: HarvestPlanBody): Promise<HarvestPlan> {
  return apiPost<HarvestPlan>('/api/v1/harvest/plans', body)
}

export async function fetchYield(farmId?: string | null): Promise<YieldRecord[]> {
  return apiGet<YieldRecord[]>('/api/v1/harvest/yield', { farmId: farmId ?? undefined })
}

export async function fetchLogisticsLoads(farmId?: string | null): Promise<LogisticsLoad[]> {
  return apiGet<LogisticsLoad[]>('/api/v1/logistics/loads', { farmId: farmId ?? undefined })
}

export async function dispatchLogisticsLoad(loadId: string): Promise<LogisticsLoad> {
  return apiPost<LogisticsLoad>('/api/v1/logistics/dispatch', { loadId })
}

export async function fetchStorageUnits(farmId?: string | null): Promise<StorageUnit[]> {
  return apiGet<StorageUnit[]>('/api/v1/storage/units', { farmId: farmId ?? undefined })
}

export async function createStorageUnit(body: StorageUnitBody): Promise<StorageUnit> {
  return apiPost<StorageUnit>('/api/v1/storage/units', body)
}

export async function patchStorageUnit(id: string, body: StorageUnitBody): Promise<StorageUnit> {
  return apiPatch<StorageUnit>(`/api/v1/storage/units/${id}`, body)
}

export async function fetchStorageLots(farmId?: string | null): Promise<StorageLot[]> {
  return apiGet<StorageLot[]>('/api/v1/storage/lots', { farmId: farmId ?? undefined })
}
