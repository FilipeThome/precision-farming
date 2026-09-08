import { apiGet } from '@/shared/api/client'
import type { InventoryItem, InventoryMovement } from '@/shared/api/types'

export async function fetchInventory(farmId?: string | null): Promise<InventoryItem[]> {
  return apiGet<InventoryItem[]>('/api/v1/inventory', { farmId: farmId ?? undefined })
}

export async function fetchInventoryMovements(itemId: string): Promise<InventoryMovement[]> {
  return apiGet<InventoryMovement[]>(`/api/v1/inventory/${itemId}/movements`)
}
