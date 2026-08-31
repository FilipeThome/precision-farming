import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { completeOperation, fetchOperations, pauseOperation, startOperation } from './api'

export const operationKeys = {
  all: (farmId?: string | null) => ['operations', farmId ?? 'all'] as const,
}

export function useOperationsQuery(farmId?: string | null) {
  return useQuery({
    queryKey: operationKeys.all(farmId),
    queryFn: () => fetchOperations(farmId),
    staleTime: 15_000,
  })
}

export function useOperationCommands() {
  const client = useQueryClient()
  const invalidate = () => client.invalidateQueries({ queryKey: ['operations'] })

  const start = useMutation({
    mutationFn: (id: string) => startOperation(id),
    onSuccess: invalidate,
  })
  const pause = useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) => pauseOperation(id, reason),
    onSuccess: invalidate,
  })
  const complete = useMutation({
    mutationFn: (id: string) => completeOperation(id),
    onSuccess: invalidate,
  })

  return { start, pause, complete }
}
