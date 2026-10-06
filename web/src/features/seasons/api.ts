import { apiGet, apiPatch, apiPost } from '@/shared/api/client'
import type { Season } from '@/features/seasons/types'

export type SeasonBody = {
  farmId: string
  name: string
  crop: string
  startDate: string
  endDate: string | null
  status: string
}

export async function fetchSeasons(farmId?: string | null): Promise<Season[]> {
  return apiGet<Season[]>('/api/v1/seasons', { farmId: farmId ?? undefined })
}

export async function createSeason(body: SeasonBody): Promise<Season> {
  return apiPost<Season>('/api/v1/seasons', body)
}

export async function patchSeason(id: string, body: SeasonBody): Promise<Season> {
  return apiPatch<Season>(`/api/v1/seasons/${id}`, body)
}
