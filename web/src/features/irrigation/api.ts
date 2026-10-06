import { apiGet, apiPatch, apiPost } from '@/shared/api/client'
import type { IrrigationAsset, IrrigationRecommendation } from '@/features/irrigation/types'

export type IrrigationAssetBody = {
  farmId: string
  fieldId: string | null
  name: string
  type: string
  status: string
  capacityMmH: number | null
}

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

export async function createIrrigationAsset(body: IrrigationAssetBody): Promise<IrrigationAsset> {
  return apiPost<IrrigationAsset>('/api/v1/irrigation/assets', body)
}

export async function patchIrrigationAsset(id: string, body: IrrigationAssetBody): Promise<IrrigationAsset> {
  return apiPatch<IrrigationAsset>(`/api/v1/irrigation/assets/${id}`, body)
}
