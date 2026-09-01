import { useQuery } from '@tanstack/react-query'

import { fetchEsg, fetchTraceability, fetchTraceabilityById } from './api'

type QueryToggle = { enabled?: boolean }

export const complianceKeys = {
  traceability: (farmId?: string | null) =>
    ['compliance', 'traceability', farmId ?? 'all'] as const,
  traceabilityEvent: (id: string) => ['compliance', 'traceability', 'event', id] as const,
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

export function useTraceabilityEventQuery(id?: string | null) {
  return useQuery({
    queryKey: complianceKeys.traceabilityEvent(id ?? ''),
    queryFn: () => fetchTraceabilityById(id!),
    staleTime: 30_000,
    enabled: Boolean(id),
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
