import { useQuery } from '@tanstack/react-query'

import { fetchHarvestPlans, fetchLogisticsLoads, fetchStorageUnits, fetchYield } from './api'

export const harvestKeys = {
  plans: (farmId?: string | null) => ['harvest', 'plans', farmId ?? 'all'] as const,
  yield: (farmId?: string | null) => ['harvest', 'yield', farmId ?? 'all'] as const,
  logistics: (farmId?: string | null) => ['harvest', 'logistics', farmId ?? 'all'] as const,
  storage: (farmId?: string | null) => ['harvest', 'storage', farmId ?? 'all'] as const,
}

export function useHarvestPlansQuery(farmId?: string | null) {
  return useQuery({
    queryKey: harvestKeys.plans(farmId),
    queryFn: () => fetchHarvestPlans(farmId),
    staleTime: 30_000,
  })
}

export function useYieldQuery(farmId?: string | null) {
  return useQuery({
    queryKey: harvestKeys.yield(farmId),
    queryFn: () => fetchYield(farmId),
    staleTime: 30_000,
  })
}

export function useLogisticsLoadsQuery(farmId?: string | null) {
  return useQuery({
    queryKey: harvestKeys.logistics(farmId),
    queryFn: () => fetchLogisticsLoads(farmId),
    staleTime: 30_000,
  })
}

export function useStorageUnitsQuery(farmId?: string | null) {
  return useQuery({
    queryKey: harvestKeys.storage(farmId),
    queryFn: () => fetchStorageUnits(farmId),
    staleTime: 30_000,
  })
}
