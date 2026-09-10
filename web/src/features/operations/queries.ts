import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import {
  completeOperation,
  fetchMachineWorkSummary,
  fetchOperations,
  pauseOperation,
  startOperation,
} from './api'

export const operationKeys = {
  all: (farmId?: string | null) => ['operations', farmId ?? 'all'] as const,
  machineSummary: (machineId: string) => ['operations', 'machine-summary', machineId] as const,
}

export function useOperationsQuery(farmId?: string | null) {
  return useQuery({
    queryKey: operationKeys.all(farmId),
    queryFn: () => fetchOperations(farmId),
    staleTime: 15_000,
  })
}

export function useMachineWorkSummaryQuery(machineId?: string | null) {
  return useQuery({
    queryKey: [...operationKeys.machineSummary(machineId ?? ''), '7d'] as const,
    queryFn: () => {
      const to = new Date()
      const from = new Date(to.getTime() - 7 * 24 * 60 * 60 * 1000)
      return fetchMachineWorkSummary(machineId!, from.toISOString(), to.toISOString())
    },
    enabled: Boolean(machineId),
    staleTime: 15_000,
  })
}

export function useOperationCommands() {
  const client = useQueryClient()
  const invalidate = () => {
    void client.invalidateQueries({ queryKey: ['operations'] })
    void client.invalidateQueries({ queryKey: ['inventory'] })
  }

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
