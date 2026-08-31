import { apiGet } from '@/shared/api/client'
import type { InventoryItem } from '@/shared/api/types'

export async function fetchInventory(farmId?: string | null): Promise<InventoryItem[]> {
  return apiGet<InventoryItem[]>('/api/v1/inventory', { farmId: farmId ?? undefined })
}
