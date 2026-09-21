import { apiGet, apiPatch, apiPost, ApiError, refreshSessionNow } from '@/shared/api/client'
import type { Farm } from '@/shared/api/types'

export type FarmBody = {
  name: string
  location: string
  areaHa: number
  timezone: string
}

export async function fetchFarms(): Promise<Farm[]> {
  return apiGet<Farm[]>('/api/v1/farms')
}

export async function createFarm(body: FarmBody): Promise<Farm> {
  const farm = await apiPost<Farm>('/api/v1/farms', body)
  const refreshed = await refreshSessionNow({ logoutOnFailure: false })
  if (!refreshed) {
    throw new ApiError(
      'Não foi possível atualizar a sessão com a nova fazenda',
      503,
      '',
      'SESSION_REFRESH_FAILED',
    )
  }
  return farm
}

export async function patchFarm(id: string, body: FarmBody): Promise<Farm> {
  return apiPatch<Farm>(`/api/v1/farms/${id}`, body)
}
