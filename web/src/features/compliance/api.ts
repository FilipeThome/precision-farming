import { apiGet } from '@/shared/api/client'
import type {
  CreditDossier,
  EsgMetric,
  EvidencePack,
  TraceabilityLot,
} from '@/shared/api/types'

export async function fetchTraceability(farmId?: string | null): Promise<TraceabilityLot[]> {
  return apiGet<TraceabilityLot[]>('/api/v1/traceability', { farmId: farmId ?? undefined })
}

export async function fetchTraceabilityById(id: string): Promise<TraceabilityLot> {
  return apiGet<TraceabilityLot>(`/api/v1/traceability/${id}`)
}

export async function fetchEsg(farmId?: string | null): Promise<EsgMetric[]> {
  return apiGet<EsgMetric[]>('/api/v1/esg', { farmId: farmId ?? undefined })
}

export async function fetchEvidencePack(lotCode: string): Promise<EvidencePack> {
  return apiGet<EvidencePack>(`/api/v1/compliance/lots/${encodeURIComponent(lotCode)}`)
}

export async function fetchCreditDossier(farmId: string): Promise<CreditDossier> {
  return apiGet<CreditDossier>(`/api/v1/compliance/farms/${encodeURIComponent(farmId)}/credit-dossier`)
}
