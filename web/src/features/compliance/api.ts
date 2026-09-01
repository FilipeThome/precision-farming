import { apiGet } from '@/shared/api/client'
import type { EsgMetric, TraceabilityLot } from '@/shared/api/types'

export async function fetchTraceability(farmId?: string | null): Promise<TraceabilityLot[]> {
  return apiGet<TraceabilityLot[]>('/api/v1/traceability', { farmId: farmId ?? undefined })
}

export async function fetchTraceabilityById(id: string): Promise<TraceabilityLot> {
  return apiGet<TraceabilityLot>(`/api/v1/traceability/${id}`)
}

export async function fetchEsg(farmId?: string | null): Promise<EsgMetric[]> {
  return apiGet<EsgMetric[]>('/api/v1/esg', { farmId: farmId ?? undefined })
}
