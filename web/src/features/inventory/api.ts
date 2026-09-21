import { apiGet, apiPatch, apiPost } from '@/shared/api/client'
import type { InventoryItem, InventoryMovement } from '@/shared/api/types'

export type InventoryCreateBody = {
  farmId: string
  name: string
  category: string
  unit: string
  quantity: number
}

export type InventoryPatchBody = {
  farmId: string
  name: string
  category: string
  unit: string
}

export async function fetchInventory(farmId?: string | null): Promise<InventoryItem[]> {
  return apiGet<InventoryItem[]>('/api/v1/inventory', { farmId: farmId ?? undefined })
}

export async function fetchInventoryMovements(itemId: string): Promise<InventoryMovement[]> {
  return apiGet<InventoryMovement[]>(`/api/v1/inventory/${itemId}/movements`)
}

export async function createInventoryItem(body: InventoryCreateBody): Promise<InventoryItem> {
  return apiPost<InventoryItem>('/api/v1/inventory', body)
}

export async function patchInventoryItem(id: string, body: InventoryPatchBody): Promise<InventoryItem> {
  return apiPatch<InventoryItem>(`/api/v1/inventory/${id}`, body)
}
