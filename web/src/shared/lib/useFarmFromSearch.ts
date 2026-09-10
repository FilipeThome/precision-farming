import { useEffect } from 'react'

import { useSearchParam } from '@/shared/lib/useSearchParam'
import { useUiStore } from '@/shared/ui/uiStore'

/** Apply `?farm=` from inspector / Control Tower links to the header farm filter. */
export function useFarmFromSearch() {
  const [farm] = useSearchParam('farm', true)
  const setFarmId = useUiStore((s) => s.setFarmId)
  useEffect(() => {
    if (farm) setFarmId(farm)
  }, [farm, setFarmId])
}

export function inspectHref(path: string, selected: string, farmId?: string | null): string {
  const params = new URLSearchParams({ selected })
  if (farmId) params.set('farm', farmId)
  return `${path}?${params.toString()}`
}
