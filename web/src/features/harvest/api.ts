import { apiGet } from '@/shared/api/client'
import type { HarvestPlan, LogisticsLoad, StorageUnit, YieldRecord } from '@/shared/api/types'

export async function fetchHarvestPlans(farmId?: string | null): Promise<HarvestPlan[]> {
  return apiGet<HarvestPlan[]>('/api/v1/harvest/plans', { farmId: farmId ?? undefined })
}

export async function fetchYield(farmId?: string | null): Promise<YieldRecord[]> {
  return apiGet<YieldRecord[]>('/api/v1/harvest/yield', { farmId: farmId ?? undefined })
}

export async function fetchLogisticsLoads(farmId?: string | null): Promise<LogisticsLoad[]> {
  return apiGet<LogisticsLoad[]>('/api/v1/logistics/loads', { farmId: farmId ?? undefined })
}

export async function fetchStorageUnits(farmId?: string | null): Promise<StorageUnit[]> {
  return apiGet<StorageUnit[]>('/api/v1/storage/units', { farmId: farmId ?? undefined })
}
