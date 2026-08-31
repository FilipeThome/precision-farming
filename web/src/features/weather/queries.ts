import { useQuery } from '@tanstack/react-query'

import { fetchForecast } from './api'

export const weatherKeys = {
  forecast: (farmId?: string | null) => ['weather', 'forecast', farmId ?? 'default'] as const,
}

export function useForecastQuery(farmId?: string | null) {
  return useQuery({
    queryKey: weatherKeys.forecast(farmId),
    queryFn: () => fetchForecast(farmId),
    staleTime: 30_000,
  })
}
