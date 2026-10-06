import { apiGet, apiPost } from '@/shared/api/client'
import type { WorkOrder } from '@/features/maintenance/types'

export type WorkOrderBody = {
  farmId: string
  machineId: string
  title: string
  priority: string
}

export async function fetchWorkOrders(farmId?: string | null): Promise<WorkOrder[]> {
  return apiGet<WorkOrder[]>('/api/v1/maintenance/work-orders', { farmId: farmId ?? undefined })
}

export async function createWorkOrder(body: WorkOrderBody): Promise<WorkOrder> {
  return apiPost<WorkOrder>('/api/v1/maintenance/work-orders', body)
}

export async function completeWorkOrder(id: string): Promise<WorkOrder> {
  return apiPost<WorkOrder>(`/api/v1/maintenance/work-orders/${id}/complete`)
}
