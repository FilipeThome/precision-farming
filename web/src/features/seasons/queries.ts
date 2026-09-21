import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { createSeason, fetchSeasons, patchSeason, type SeasonBody } from './api'

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

export function useSeasonCommands() {
  const client = useQueryClient()
  const invalidate = () => void client.invalidateQueries({ queryKey: ['seasons'] })
  const create = useMutation({ mutationFn: createSeason, onSuccess: invalidate })
  const patch = useMutation({
    mutationFn: ({ id, body }: { id: string; body: SeasonBody }) => patchSeason(id, body),
    onSuccess: invalidate,
  })
  return { create, patch }
}
