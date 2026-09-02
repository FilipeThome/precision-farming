import { apiGet } from '@/shared/api/client'
import type { IrrigationAsset, IrrigationRecommendation } from '@/shared/api/types'

export async function fetchIrrigationAssets(farmId?: string | null): Promise<IrrigationAsset[]> {
  return apiGet<IrrigationAsset[]>('/api/v1/irrigation/assets', { farmId: farmId ?? undefined })
}

export async function fetchIrrigationRecommendations(
  farmId?: string | null,
): Promise<IrrigationRecommendation[]> {
  return apiGet<IrrigationRecommendation[]>('/api/v1/irrigation/recommendations', {
    farmId: farmId ?? undefined,
  })
}
