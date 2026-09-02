import { useQuery } from '@tanstack/react-query'

import { fetchSeasons } from './api'

export const seasonsKeys = {
  all: (farmId?: string | null) => ['seasons', farmId ?? 'all'] as const,
}

export function useSeasonsQuery(farmId?: string | null) {
  return useQuery({
    queryKey: seasonsKeys.all(farmId),
    queryFn: () => fetchSeasons(farmId),
    staleTime: 30_000,
  })
}
