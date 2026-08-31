import { useQuery } from '@tanstack/react-query'

import { fetchFarms } from './api'

export const farmKeys = {
  all: ['farms'] as const,
}

export function useFarmsQuery() {
  return useQuery({
    queryKey: farmKeys.all,
    queryFn: fetchFarms,
    staleTime: 30_000,
  })
}
