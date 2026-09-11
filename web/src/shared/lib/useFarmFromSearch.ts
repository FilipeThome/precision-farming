import { useSearchParam } from '@/shared/lib/useSearchParam'
import { useUiStore } from '@/shared/ui/uiStore'

/** Apply `?farm=` from inspector / Control Tower links before the page reads the store. */
export function useFarmFromSearch() {
  const [farm] = useSearchParam('farm', true)
  if (farm && useUiStore.getState().farmId !== farm) {
    useUiStore.getState().setFarmId(farm)
  }
}

export function inspectHref(path: string, selected: string, farmId?: string | null): string {
  const params = new URLSearchParams({ selected })
  if (farmId) params.set('farm', farmId)
  return `${path}?${params.toString()}`
}
