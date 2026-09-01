import { apiGet } from '@/shared/api/client'
import type { Season } from '@/shared/api/types'

export async function fetchSeasons(farmId?: string | null): Promise<Season[]> {
  return apiGet<Season[]>('/api/v1/seasons', { farmId: farmId ?? undefined })
}
