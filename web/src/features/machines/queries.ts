import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { fetchMachines, fetchMachineMetrics, fetchMachineTelemetry, saveMachine } from './api'

export const machineKeys = {
  all: (farmId?: string | null) => ['machines', farmId ?? 'all'] as const,
  telemetry: (machineId: string) => ['machines', machineId, 'telemetry'] as const,
  metrics: (machineId: string) => ['machines', machineId, 'metrics'] as const,
}

export function useMachinesQuery(farmId?: string | null) {
  return useQuery({
    queryKey: machineKeys.all(farmId),
    queryFn: () => fetchMachines(farmId),
    staleTime: 30_000,
  })
}

export function useMachineTelemetryQuery(machineId?: string | null) {
  return useQuery({
    queryKey: machineKeys.telemetry(machineId ?? ''),
    queryFn: () => fetchMachineTelemetry(machineId!),
    enabled: Boolean(machineId),
    retry: 1,
    staleTime: 30_000,
  })
}

export function useMachineMetricsQuery(machineId?: string | null) {
  return useQuery({
    queryKey: machineKeys.metrics(machineId ?? ''),
    queryFn: () => fetchMachineMetrics(machineId!),
    enabled: Boolean(machineId),
    retry: 1,
    staleTime: 30_000,
  })
}

export function useMachineCommands() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: saveMachine,
    onSuccess: () => {
      void client.invalidateQueries({ queryKey: ['machines'] })
    },
  })
}
