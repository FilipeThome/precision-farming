import { useQuery } from '@tanstack/react-query'

import { fetchEsg, fetchTraceability } from './api'

export const complianceKeys = {
  traceability: (farmId?: string | null) =>
    ['compliance', 'traceability', farmId ?? 'all'] as const,
  esg: (farmId?: string | null) => ['compliance', 'esg', farmId ?? 'all'] as const,
}

export function useTraceabilityQuery(farmId?: string | null) {
  return useQuery({
    queryKey: complianceKeys.traceability(farmId),
    queryFn: () => fetchTraceability(farmId),
    staleTime: 30_000,
  })
}

export function useEsgQuery(farmId?: string | null) {
  return useQuery({
    queryKey: complianceKeys.esg(farmId),
    queryFn: () => fetchEsg(farmId),
    staleTime: 30_000,
  })
}
