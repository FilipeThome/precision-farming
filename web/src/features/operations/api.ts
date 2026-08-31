import { apiGet, apiPost } from '@/shared/api/client'
import type { Operation } from '@/shared/api/types'

export async function fetchOperations(farmId?: string | null): Promise<Operation[]> {
  return apiGet<Operation[]>('/api/v1/operations', { farmId: farmId ?? undefined })
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
