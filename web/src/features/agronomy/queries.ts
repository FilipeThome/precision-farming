import { useQuery } from '@tanstack/react-query'

import { fetchRecommendations, fetchScouting, fetchSoilSamples } from './api'

export const agronomyKeys = {
  scouting: (farmId?: string | null) => ['agronomy', 'scouting', farmId ?? 'all'] as const,
  soil: (farmId?: string | null) => ['agronomy', 'soil', farmId ?? 'all'] as const,
  recommendations: (farmId?: string | null) =>
    ['agronomy', 'recommendations', farmId ?? 'all'] as const,
}

export function useScoutingQuery(farmId?: string | null) {
  return useQuery({
    queryKey: agronomyKeys.scouting(farmId),
    queryFn: () => fetchScouting(farmId),
    staleTime: 30_000,
  })
}

export function useSoilSamplesQuery(farmId?: string | null) {
  return useQuery({
    queryKey: agronomyKeys.soil(farmId),
    queryFn: () => fetchSoilSamples(farmId),
    staleTime: 30_000,
  })
}

export function useRecommendationsQuery(farmId?: string | null) {
  return useQuery({
    queryKey: agronomyKeys.recommendations(farmId),
    queryFn: () => fetchRecommendations(farmId),
    staleTime: 30_000,
  })
}
