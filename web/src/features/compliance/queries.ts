import { useQuery } from '@tanstack/react-query'

import {
  fetchCreditDossier,
  fetchEsg,
  fetchEvidencePack,
  fetchTraceability,
  fetchTraceabilityById,
} from './api'

type QueryToggle = { enabled?: boolean }

export const complianceKeys = {
  traceability: (farmId?: string | null) =>
    ['compliance', 'traceability', farmId ?? 'all'] as const,
  traceabilityEvent: (id: string) => ['compliance', 'traceability', 'event', id] as const,
  esg: (farmId?: string | null) => ['compliance', 'esg', farmId ?? 'all'] as const,
  evidence: (lotCode: string) => ['compliance', 'evidence', lotCode] as const,
  creditDossier: (farmId: string) => ['compliance', 'credit-dossier', farmId] as const,
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

export function useEvidencePackQuery(lotCode?: string | null, options?: QueryToggle) {
  return useQuery({
    queryKey: complianceKeys.evidence(lotCode ?? ''),
    queryFn: () => fetchEvidencePack(lotCode!),
    staleTime: 30_000,
    enabled: (options?.enabled ?? true) && Boolean(lotCode),
  })
}

export function useCreditDossierQuery(farmId?: string | null, options?: QueryToggle) {
  return useQuery({
    queryKey: complianceKeys.creditDossier(farmId ?? ''),
    queryFn: () => fetchCreditDossier(farmId!),
    staleTime: 30_000,
    enabled: (options?.enabled ?? true) && Boolean(farmId),
  })
}
