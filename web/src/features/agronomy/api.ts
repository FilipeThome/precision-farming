import { apiGet } from '@/shared/api/client'
import type { AgronomyRecommendation, ScoutingRecord, SoilSample } from '@/shared/api/types'

export async function fetchScouting(farmId?: string | null): Promise<ScoutingRecord[]> {
  return apiGet<ScoutingRecord[]>('/api/v1/scouting', { farmId: farmId ?? undefined })
}

export async function fetchSoilSamples(farmId?: string | null): Promise<SoilSample[]> {
  return apiGet<SoilSample[]>('/api/v1/soil/samples', { farmId: farmId ?? undefined })
}

export async function fetchRecommendations(farmId?: string | null): Promise<AgronomyRecommendation[]> {
  return apiGet<AgronomyRecommendation[]>('/api/v1/recommendations', { farmId: farmId ?? undefined })
}
