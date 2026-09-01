import { useQuery } from '@tanstack/react-query'

import { fetchIrrigationAssets, fetchIrrigationRecommendations } from './api'

export const irrigationKeys = {
  assets: (farmId?: string | null) => ['irrigation', 'assets', farmId ?? 'all'] as const,
  recommendations: (farmId?: string | null) =>
    ['irrigation', 'recommendations', farmId ?? 'all'] as const,
}

export function useIrrigationAssetsQuery(farmId?: string | null) {
  return useQuery({
    queryKey: irrigationKeys.assets(farmId),
    queryFn: () => fetchIrrigationAssets(farmId),
    staleTime: 30_000,
  })
}

export function useIrrigationRecommendationsQuery(farmId?: string | null) {
  return useQuery({
    queryKey: irrigationKeys.recommendations(farmId),
    queryFn: () => fetchIrrigationRecommendations(farmId),
    staleTime: 30_000,
  })
}
