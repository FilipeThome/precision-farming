import { apiGet } from '@/shared/api/client'
import type { Farm } from '@/shared/api/types'

export async function fetchFarms(): Promise<Farm[]> {
  return apiGet<Farm[]>('/api/v1/farms')
}
