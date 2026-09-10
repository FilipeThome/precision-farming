import { apiGet, apiPost } from '@/shared/api/client'
import type { MachineWorkSummary, Operation } from '@/shared/api/types'

export async function fetchOperations(farmId?: string | null): Promise<Operation[]> {
  return apiGet<Operation[]>('/api/v1/operations', { farmId: farmId ?? undefined })
}

export async function fetchMachineWorkSummary(
  machineId: string,
  from?: string,
  to?: string,
): Promise<MachineWorkSummary> {
  return apiGet<MachineWorkSummary>('/api/v1/operations/machine-summary', { machineId, from, to })
}

export async function startOperation(id: string): Promise<Operation> {
  return apiPost<Operation>(`/api/v1/operations/${id}/start`)
}

export async function pauseOperation(id: string, reason: string): Promise<Operation> {
  return apiPost<Operation>(`/api/v1/operations/${id}/pause`, { reason })
}

export async function completeOperation(id: string): Promise<Operation> {
  return apiPost<Operation>(`/api/v1/operations/${id}/complete`)
}
