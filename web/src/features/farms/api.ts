import { apiGet, apiPatch, apiPost, ApiError, refreshSessionNow } from '@/shared/api/client'
import type { Farm } from '@/features/farms/types'
import { useAuthStore } from '@/shared/auth/store'

export type FarmBody = {
  name: string
  location: string
  areaHa: number
  timezone: string
}

export async function fetchFarms(): Promise<Farm[]> {
  return apiGet<Farm[]>('/api/v1/farms')
}

/** Session-scoped cache so a failed JWT refresh can retry without POSTing a second farm. */
const pendingFarmCreates = new Map<string, Farm>()

function canonicalJson(body: FarmBody): string {
  return JSON.stringify({ name: body.name, location: body.location, areaHa: body.areaHa, timezone: body.timezone })
}

function pendingFarmKey(userId: string, body: FarmBody): string {
  return `${userId}:${canonicalJson(body)}`
}

function sessionFarmKey(userId: string, body: FarmBody): string {
  return `pf:pending-farm:${userId}:${canonicalJson(body)}`
}

useAuthStore.subscribe((state, prev) => {
  if (state.userId !== prev.userId) pendingFarmCreates.clear()
})

function readSessionFarm(key: string): Farm | undefined {
  try {
    const raw = sessionStorage.getItem(key)
    if (!raw) return undefined
    return JSON.parse(raw) as Farm
  } catch {
    return undefined
  }
}

function writeSessionFarm(key: string, farm: Farm): void {
  try {
    sessionStorage.setItem(key, JSON.stringify(farm))
  } catch {
    // Quota or private mode: keep the in-memory map only.
  }
}

function deleteSessionFarm(key: string): void {
  try {
    sessionStorage.removeItem(key)
  } catch {
    // ignore
  }
}

function lookupPending(userId: string | null, body: FarmBody): Farm | undefined {
  if (!userId) return undefined
  return pendingFarmCreates.get(pendingFarmKey(userId, body)) ?? readSessionFarm(sessionFarmKey(userId, body))
}

function persistPending(userId: string | null, body: FarmBody, farm: Farm): void {
  if (!userId) return
  pendingFarmCreates.set(pendingFarmKey(userId, body), farm)
  writeSessionFarm(sessionFarmKey(userId, body), farm)
}

function clearPending(userId: string | null, body: FarmBody): void {
  if (!userId) return
  pendingFarmCreates.delete(pendingFarmKey(userId, body))
  deleteSessionFarm(sessionFarmKey(userId, body))
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
  const userId = useAuthStore.getState().userId
  const farm = lookupPending(userId, body) || (await apiPost<Farm>('/api/v1/farms', body))
  persistPending(userId, body, farm)
  // Refresh tokens are single-use; never retry the same token (reuse triggers revokeAll).
  const refreshed = await refreshSessionNow({ logoutOnFailure: false })
  if (!refreshed) {
    persistPending(userId, body, farm)
    throw sessionRefreshFailed()
  }
  clearPending(userId, body)
  return farm
}

export async function patchFarm(id: string, body: FarmBody): Promise<Farm> {
  return apiPatch<Farm>(`/api/v1/farms/${id}`, body)
}
