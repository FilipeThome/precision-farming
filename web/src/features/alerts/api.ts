import { apiGet, apiPost } from '@/shared/api/client'
import type { Alert } from '@/features/alerts/types'

export async function fetchAlerts(farmId?: string | null): Promise<Alert[]> {
  return apiGet<Alert[]>('/api/v1/alerts', { farmId: farmId ?? undefined })
}

export async function ackAlert(id: string): Promise<Alert> {
  return apiPost<Alert>(`/api/v1/alerts/${id}/ack`)
}
