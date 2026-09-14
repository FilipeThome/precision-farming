import { useEffect } from 'react'

import { useSearchParam } from '@/shared/lib/useSearchParam'
import { useUiStore } from '@/shared/ui/uiStore'

/** Keep `?farm=` and the UI store in sync so inspect links and the selector cannot fight. */
export function useFarmFromSearch() {
  const [farm, setFarm] = useSearchParam('farm', true)
  const storeFarm = useUiStore((s) => s.farmId)
  if (farm && useUiStore.getState().farmId !== farm) {
    useUiStore.getState().setFarmId(farm)
  }
  useEffect(() => {
    if (!farm && storeFarm) setFarm(storeFarm)
  }, [farm, storeFarm, setFarm])
}

export function inspectHref(path: string, selected: string, farmId?: string | null): string {
  const params = new URLSearchParams({ selected })
  if (farmId) params.set('farm', farmId)
  return `${path}?${params.toString()}`
}

/** Path with the current farm context, or the bare path when none is selected. */
export function farmHref(path: string, farmId?: string | null): string {
  if (!farmId) return path
  const params = new URLSearchParams({ farm: farmId })
  return `${path}?${params.toString()}`
}
