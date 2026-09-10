import { apiGet } from '@/shared/api/client'
import type { Machine, MachineMetrics, TelemetryPoint } from '@/shared/api/types'

export async function fetchMachines(farmId?: string | null): Promise<Machine[]> {
  return apiGet<Machine[]>('/api/v1/machines', { farmId: farmId ?? undefined })
}

export async function fetchMachineTelemetry(machineId: string): Promise<TelemetryPoint[]> {
  return apiGet<TelemetryPoint[]>(`/api/v1/machines/${machineId}/telemetry`)
}

export async function fetchMachineMetrics(machineId: string): Promise<MachineMetrics> {
  return apiGet<MachineMetrics>(`/api/v1/machines/${machineId}/metrics`)
}
