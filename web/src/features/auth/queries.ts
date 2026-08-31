import { useMutation, useQuery } from '@tanstack/react-query'

import { fetchMe, loginRequest } from './api'

export const authKeys = {
  me: ['auth', 'me'] as const,
}

export function useLoginMutation() {
  return useMutation({
    mutationFn: ({ email, password }: { email: string; password: string }) =>
      loginRequest(email, password),
  })
}

export function useMeQuery(enabled: boolean) {
  return useQuery({
    queryKey: authKeys.me,
    queryFn: fetchMe,
    enabled,
  })
}
