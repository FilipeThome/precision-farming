import { useQuery } from '@tanstack/react-query'

import {
  fetchForecast,
  fetchParametricIndex,
  fetchPlantingGate,
  fetchWeatherWindows,
} from './api'

type QueryToggle = { enabled?: boolean }

export const weatherKeys = {
  forecast: (farmId?: string | null) => ['weather', 'forecast', farmId ?? 'default'] as const,
  windows: (farmId?: string | null) => ['weather', 'windows', farmId ?? 'all'] as const,
  plantingGate: (farmId: string, date: string) =>
    ['weather', 'planting-gate', farmId, date] as const,
  parametricIndex: (farmId: string) => ['weather', 'parametric-index', farmId] as const,
}

export function useForecastQuery(farmId?: string | null, options?: QueryToggle) {
  return useQuery({
    queryKey: weatherKeys.forecast(farmId),
    queryFn: () => fetchForecast(farmId),
    staleTime: 30_000,
    enabled: options?.enabled ?? true,
  })
}

export function useWeatherWindowsQuery(farmId?: string | null, options?: QueryToggle) {
  return useQuery({
    queryKey: weatherKeys.windows(farmId),
    queryFn: () => fetchWeatherWindows(farmId),
    staleTime: 30_000,
    enabled: options?.enabled ?? true,
  })
}

export function usePlantingGateQuery(
  farmId?: string | null,
  date?: string | null,
  crop = 'SOY',
  options?: QueryToggle,
) {
  return useQuery({
    queryKey: weatherKeys.plantingGate(farmId ?? '', date ?? ''),
    queryFn: () => fetchPlantingGate(farmId!, date!, crop),
    staleTime: 30_000,
    enabled: (options?.enabled ?? true) && Boolean(farmId) && Boolean(date),
  })
}

export function useParametricIndexQuery(farmId?: string | null, options?: QueryToggle) {
  return useQuery({
    queryKey: weatherKeys.parametricIndex(farmId ?? ''),
    queryFn: () => fetchParametricIndex(farmId!),
    staleTime: 30_000,
    enabled: (options?.enabled ?? true) && Boolean(farmId),
  })
}
