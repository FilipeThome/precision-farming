import { useQuery } from '@tanstack/react-query'

import { fetchInsights, fetchMachineRisk } from './api'

export const aiKeys = {
  insights: (farmId?: string | null) => ['ai', 'insights', farmId ?? 'all'] as const,
  machineRisk: (machineId: string) => ['ai', 'machine-risk', machineId] as const,
}

export function useInsightsQuery(farmId?: string | null) {
  return useQuery({
    queryKey: aiKeys.insights(farmId),
    queryFn: () => fetchInsights(farmId),
    staleTime: 30_000,
  })
}

export function useMachineRiskQuery(machineId: string, enabled: boolean) {
  return useQuery({
    queryKey: aiKeys.machineRisk(machineId),
    queryFn: () => fetchMachineRisk(machineId),
    enabled,
    staleTime: 30_000,
  })
}
