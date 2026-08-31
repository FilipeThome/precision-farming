import { apiGet } from '@/shared/api/client'
import type { Machine } from '@/shared/api/types'

export async function fetchMachines(farmId?: string | null): Promise<Machine[]> {
  return apiGet<Machine[]>('/api/v1/machines', { farmId: farmId ?? undefined })
}
