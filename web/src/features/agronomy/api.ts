import { apiGet, apiPost } from '@/shared/api/client'
import type {
  AgronomyRecommendation,
  MoaRotation,
  Prescription,
  ScoutingRecord,
  SoilSample,
  SpraySavings,
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

export async function fetchPrescription(id: string): Promise<Prescription> {
  return apiGet<Prescription>(`/api/v1/prescriptions/${id}`)
}

export async function approvePrescription(id: string): Promise<Prescription> {
  return apiPost<Prescription>(`/api/v1/prescriptions/${id}/approve`)
}

export async function rejectPrescription(id: string): Promise<Prescription> {
  return apiPost<Prescription>(`/api/v1/prescriptions/${id}/reject`)
}

export async function fetchSpraySavings(prescriptionId: string): Promise<SpraySavings> {
  return apiGet<SpraySavings>(`/api/v1/prescriptions/${prescriptionId}/spray-savings`)
}

export async function fetchMoaRotation(fieldId: string): Promise<MoaRotation> {
  return apiGet<MoaRotation>('/api/v1/prescriptions/moa-rotation', { fieldId })
}
