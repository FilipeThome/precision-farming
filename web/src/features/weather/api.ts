import { apiGet } from '@/shared/api/client'
import type { ParametricIndex, PlantingGate, WeatherForecast, WeatherWindow } from '@/features/weather/types'

export async function fetchForecast(farmId?: string | null): Promise<WeatherForecast[]> {
  return apiGet<WeatherForecast[]>('/api/v1/weather/forecast', { farmId: farmId ?? undefined })
}

export async function fetchWeatherWindows(farmId?: string | null): Promise<WeatherWindow[]> {
  return apiGet<WeatherWindow[]>('/api/v1/weather/windows', { farmId: farmId ?? undefined })
}

export async function fetchPlantingGate(
  farmId: string,
  date: string,
  crop = 'SOY',
): Promise<PlantingGate> {
  return apiGet<PlantingGate>('/api/v1/weather/planting-gate', { farmId, date, crop })
}

export async function fetchParametricIndex(farmId: string): Promise<ParametricIndex> {
  return apiGet<ParametricIndex>('/api/v1/weather/parametric-index', { farmId })
}
