import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { createField, fetchFields, patchField, type FieldBody } from './api'

export const fieldKeys = {
  all: (farmId?: string | null) => ['fields', farmId ?? 'all'] as const,
}

export function useFieldsQuery(farmId?: string | null) {
  return useQuery({
    queryKey: fieldKeys.all(farmId),
    queryFn: () => fetchFields(farmId),
    staleTime: 30_000,
  })
}

export function useFieldCommands() {
  const client = useQueryClient()
  const invalidate = () => void client.invalidateQueries({ queryKey: ['fields'] })
  const create = useMutation({ mutationFn: createField, onSuccess: invalidate })
  const patch = useMutation({
    mutationFn: ({ id, body }: { id: string; body: FieldBody }) => patchField(id, body),
    onSuccess: invalidate,
  })
  return { create, patch }
}
