import { apiGet, apiPost } from '@/shared/api/client'
import type {
  AgronomyRecommendation,
  Prescription,
  ScoutingRecord,
  SoilSample,
} from '@/shared/api/types'

export async function fetchScouting(farmId?: string | null): Promise<ScoutingRecord[]> {
  return apiGet<ScoutingRecord[]>('/api/v1/scouting', { farmId: farmId ?? undefined })
}

export async function fetchSoilSamples(farmId?: string | null): Promise<SoilSample[]> {
  return apiGet<SoilSample[]>('/api/v1/soil/samples', { farmId: farmId ?? undefined })
}

export async function fetchRecommendations(farmId?: string | null): Promise<AgronomyRecommendation[]> {
  return apiGet<AgronomyRecommendation[]>('/api/v1/recommendations', { farmId: farmId ?? undefined })
}

export async function fetchPrescriptions(farmId?: string | null): Promise<Prescription[]> {
  return apiGet<Prescription[]>('/api/v1/prescriptions', { farmId: farmId ?? undefined })
}

export async function approvePrescription(id: string): Promise<Prescription> {
  return apiPost<Prescription>(`/api/v1/prescriptions/${id}/approve`)
}
