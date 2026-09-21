import { useQuery } from '@tanstack/react-query'
import { useEffect, useState } from 'react'

import { apiGetBlob } from '@/shared/api/client'

export function useMachinePhoto(photoFileId?: string | null) {
  const query = useQuery({
    queryKey: ['files', 'content', photoFileId],
    queryFn: async () => apiGetBlob(`/api/v1/files/${photoFileId}/content`),
    enabled: Boolean(photoFileId),
    staleTime: 5 * 60_000,
  })
  const [url, setUrl] = useState<string | null>(null)
  useEffect(() => {
    if (!query.data) {
      setUrl(null)
      return
    }
    const next = URL.createObjectURL(query.data)
    setUrl(next)
    return () => URL.revokeObjectURL(next)
  }, [query.data])
  return { ...query, data: url }
}
