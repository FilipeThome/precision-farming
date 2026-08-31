import { apiGet } from '@/shared/api/client'
import type { WeatherForecast } from '@/shared/api/types'

export async function fetchForecast(farmId?: string | null): Promise<WeatherForecast[]> {
  return apiGet<WeatherForecast[]>('/api/v1/weather/forecast', { farmId: farmId ?? undefined })
}
