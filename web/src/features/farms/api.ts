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

const REFRESH_ATTEMPTS = 3

let pendingFarmCreate: { key: string; farm: Farm } | null = null

function farmBodyKey(body: FarmBody): string {
  return JSON.stringify(body)
}

async function refreshCreatedFarmSession(): Promise<boolean> {
  for (let i = 0; i < REFRESH_ATTEMPTS; i++) {
    if (await refreshSessionNow({ logoutOnFailure: false })) return true
  }
  return false
}

function sessionRefreshFailed(): ApiError {
  return new ApiError(
    'Não foi possível atualizar a sessão com a nova fazenda',
    503,
    '',
    'SESSION_REFRESH_FAILED',
  )
}

export async function createFarm(body: FarmBody): Promise<Farm> {
  const key = farmBodyKey(body)
  const farm =
    pendingFarmCreate?.key === key
      ? pendingFarmCreate.farm
      : await apiPost<Farm>('/api/v1/farms', body)
  const refreshed = await refreshCreatedFarmSession()
  if (!refreshed) {
    pendingFarmCreate = { key, farm }
    throw sessionRefreshFailed()
  }
  pendingFarmCreate = null
  return farm
}

export async function patchFarm(id: string, body: FarmBody): Promise<Farm> {
  return apiPatch<Farm>(`/api/v1/farms/${id}`, body)
}
