import { useSearchParam } from '@/shared/lib/useSearchParam'

export function useSelectedId() {
  const [selectedId, setSelectedId] = useSearchParam('selected', true)
  return { selectedId, setSelectedId }
}
