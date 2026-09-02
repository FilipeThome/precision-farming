import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import {
  dispatchLogisticsLoad,
  fetchHarvestPlans,
  fetchLogisticsLoads,
  fetchStorageLots,
  fetchStorageUnits,
  fetchYield,
} from './api'

type QueryToggle = { enabled?: boolean }

export const harvestKeys = {
  plans: (farmId?: string | null) => ['harvest', 'plans', farmId ?? 'all'] as const,
  yield: (farmId?: string | null) => ['harvest', 'yield', farmId ?? 'all'] as const,
  logistics: (farmId?: string | null) => ['harvest', 'logistics', farmId ?? 'all'] as const,
  storage: (farmId?: string | null) => ['harvest', 'storage', farmId ?? 'all'] as const,
  lots: (farmId?: string | null) => ['harvest', 'lots', farmId ?? 'all'] as const,
}

export function useHarvestPlansQuery(farmId?: string | null, options?: QueryToggle) {
  return useQuery({
    queryKey: harvestKeys.plans(farmId),
    queryFn: () => fetchHarvestPlans(farmId),
    staleTime: 30_000,
    enabled: options?.enabled ?? true,
  })
}

export function useYieldQuery(farmId?: string | null, options?: QueryToggle) {
  return useQuery({
    queryKey: harvestKeys.yield(farmId),
    queryFn: () => fetchYield(farmId),
    staleTime: 30_000,
    enabled: options?.enabled ?? true,
  })
}

export function useLogisticsLoadsQuery(farmId?: string | null, options?: QueryToggle) {
  return useQuery({
    queryKey: harvestKeys.logistics(farmId),
    queryFn: () => fetchLogisticsLoads(farmId),
    staleTime: 30_000,
    enabled: options?.enabled ?? true,
  })
}

export function useStorageUnitsQuery(farmId?: string | null, options?: QueryToggle) {
  return useQuery({
    queryKey: harvestKeys.storage(farmId),
    queryFn: () => fetchStorageUnits(farmId),
    staleTime: 30_000,
    enabled: options?.enabled ?? true,
  })
}

export function useStorageLotsQuery(farmId?: string | null, options?: QueryToggle) {
  return useQuery({
    queryKey: harvestKeys.lots(farmId),
    queryFn: () => fetchStorageLots(farmId),
    staleTime: 30_000,
    enabled: options?.enabled ?? true,
  })
}

export function useDispatchLoad() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: (loadId: string) => dispatchLogisticsLoad(loadId),
    onSuccess: () => {
      void client.invalidateQueries({ queryKey: ['harvest', 'logistics'] })
    },
  })
}
