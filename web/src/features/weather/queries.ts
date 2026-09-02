import { useQuery } from '@tanstack/react-query'

import { fetchForecast, fetchWeatherWindows } from './api'

type QueryToggle = { enabled?: boolean }

export const weatherKeys = {
  forecast: (farmId?: string | null) => ['weather', 'forecast', farmId ?? 'default'] as const,
  windows: (farmId?: string | null) => ['weather', 'windows', farmId ?? 'all'] as const,
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
