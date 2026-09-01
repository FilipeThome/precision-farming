import { useQuery } from '@tanstack/react-query'

import { fetchMapLayers } from './api'

export const mapKeys = {
  layers: (farmId?: string | null) => ['map', 'layers', farmId ?? 'all'] as const,
}

export function useMapLayersQuery(farmId?: string | null) {
  return useQuery({
    queryKey: mapKeys.layers(farmId),
    queryFn: () => fetchMapLayers(farmId),
    staleTime: 60_000,
  })
}
