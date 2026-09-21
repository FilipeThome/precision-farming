import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import {
  createIrrigationAsset,
  fetchIrrigationAssets,
  fetchIrrigationRecommendations,
  patchIrrigationAsset,
  type IrrigationAssetBody,
} from './api'

type QueryToggle = { enabled?: boolean }

export const irrigationKeys = {
  assets: (farmId?: string | null) => ['irrigation', 'assets', farmId ?? 'all'] as const,
  recommendations: (farmId?: string | null) =>
    ['irrigation', 'recommendations', farmId ?? 'all'] as const,
}

export function useIrrigationAssetsQuery(farmId?: string | null, options?: QueryToggle) {
  return useQuery({
    queryKey: irrigationKeys.assets(farmId),
    queryFn: () => fetchIrrigationAssets(farmId),
    staleTime: 30_000,
    enabled: options?.enabled ?? true,
  })
}

export function useIrrigationRecommendationsQuery(farmId?: string | null, options?: QueryToggle) {
  return useQuery({
    queryKey: irrigationKeys.recommendations(farmId),
    queryFn: () => fetchIrrigationRecommendations(farmId),
    staleTime: 30_000,
    enabled: options?.enabled ?? true,
  })
}

export function useIrrigationAssetCommands() {
  const client = useQueryClient()
  const invalidate = () => void client.invalidateQueries({ queryKey: ['irrigation', 'assets'] })
  const create = useMutation({ mutationFn: createIrrigationAsset, onSuccess: invalidate })
  const patch = useMutation({
    mutationFn: ({ id, body }: { id: string; body: IrrigationAssetBody }) => patchIrrigationAsset(id, body),
    onSuccess: invalidate,
  })
  return { create, patch }
}
