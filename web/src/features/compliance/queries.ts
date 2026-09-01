import { useQuery } from '@tanstack/react-query'

import { fetchEsg, fetchTraceability } from './api'

type QueryToggle = { enabled?: boolean }

export const complianceKeys = {
  traceability: (farmId?: string | null) =>
    ['compliance', 'traceability', farmId ?? 'all'] as const,
  esg: (farmId?: string | null) => ['compliance', 'esg', farmId ?? 'all'] as const,
}

export function useTraceabilityQuery(farmId?: string | null, options?: QueryToggle) {
  return useQuery({
    queryKey: complianceKeys.traceability(farmId),
    queryFn: () => fetchTraceability(farmId),
    staleTime: 30_000,
    enabled: options?.enabled ?? true,
  })
}

export function useEsgQuery(farmId?: string | null, options?: QueryToggle) {
  return useQuery({
    queryKey: complianceKeys.esg(farmId),
    queryFn: () => fetchEsg(farmId),
    staleTime: 30_000,
    enabled: options?.enabled ?? true,
  })
}
