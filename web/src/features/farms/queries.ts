import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { createFarm, fetchFarms, patchFarm, type FarmBody } from './api'

export const farmKeys = {
  all: ['farms'] as const,
}

export function useFarmsQuery() {
  return useQuery({
    queryKey: farmKeys.all,
    queryFn: fetchFarms,
    staleTime: 30_000,
  })
}

export function useFarmCommands() {
  const client = useQueryClient()
  const invalidate = () => {
    void client.invalidateQueries({ queryKey: ['farms'] })
  }
  const create = useMutation({ mutationFn: createFarm, onSuccess: invalidate })
  const patch = useMutation({
    mutationFn: ({ id, body }: { id: string; body: FarmBody }) => patchFarm(id, body),
    onSuccess: invalidate,
  })
  return { create, patch }
}
