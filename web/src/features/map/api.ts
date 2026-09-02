import { apiGet } from '@/shared/api/client'
import type { MapLayer } from '@/shared/api/types'

export async function fetchMapLayers(farmId?: string | null): Promise<MapLayer[]> {
  return apiGet<MapLayer[]>('/api/v1/map/layers', { farmId: farmId ?? undefined })
}
