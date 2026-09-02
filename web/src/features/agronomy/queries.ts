import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import {
  approvePrescription,
  fetchPrescriptions,
  fetchRecommendations,
  fetchScouting,
  fetchSoilSamples,
} from './api'

type QueryToggle = { enabled?: boolean }

export const agronomyKeys = {
  scouting: (farmId?: string | null) => ['agronomy', 'scouting', farmId ?? 'all'] as const,
  soil: (farmId?: string | null) => ['agronomy', 'soil', farmId ?? 'all'] as const,
  recommendations: (farmId?: string | null) =>
    ['agronomy', 'recommendations', farmId ?? 'all'] as const,
  prescriptions: (farmId?: string | null) =>
    ['agronomy', 'prescriptions', farmId ?? 'all'] as const,
}

export function useScoutingQuery(farmId?: string | null, options?: QueryToggle) {
  return useQuery({
    queryKey: agronomyKeys.scouting(farmId),
    queryFn: () => fetchScouting(farmId),
    staleTime: 30_000,
    enabled: options?.enabled ?? true,
  })
}

export function useSoilSamplesQuery(farmId?: string | null, options?: QueryToggle) {
  return useQuery({
    queryKey: agronomyKeys.soil(farmId),
    queryFn: () => fetchSoilSamples(farmId),
    staleTime: 30_000,
    enabled: options?.enabled ?? true,
  })
}

export function useRecommendationsQuery(farmId?: string | null, options?: QueryToggle) {
  return useQuery({
    queryKey: agronomyKeys.recommendations(farmId),
    queryFn: () => fetchRecommendations(farmId),
    staleTime: 30_000,
    enabled: options?.enabled ?? true,
  })
}

export function usePrescriptionsQuery(farmId?: string | null, options?: QueryToggle) {
  return useQuery({
    queryKey: agronomyKeys.prescriptions(farmId),
    queryFn: () => fetchPrescriptions(farmId),
    staleTime: 30_000,
    enabled: options?.enabled ?? true,
  })
}

export function useApprovePrescription() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => approvePrescription(id),
    onSuccess: () => {
      void client.invalidateQueries({ queryKey: ['agronomy', 'prescriptions'] })
    },
  })
}
