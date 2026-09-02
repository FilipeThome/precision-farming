import { apiGet } from '@/shared/api/client'
import type { WeatherForecast, WeatherWindow } from '@/shared/api/types'

export async function fetchForecast(farmId?: string | null): Promise<WeatherForecast[]> {
  return apiGet<WeatherForecast[]>('/api/v1/weather/forecast', { farmId: farmId ?? undefined })
}

export async function fetchWeatherWindows(farmId?: string | null): Promise<WeatherWindow[]> {
  return apiGet<WeatherWindow[]>('/api/v1/weather/windows', { farmId: farmId ?? undefined })
}
