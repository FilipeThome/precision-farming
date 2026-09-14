import { useQueryClient, type QueryClient } from '@tanstack/react-query'
import { useCallback, useMemo, useSyncExternalStore } from 'react'

import type { Farm, Field, Machine } from '@/shared/api/types'
import { isUuid } from '@/shared/i18n/domainLabels'

type Named = { id: string; name?: string | null }

const ROOT_KEYS = ['farms', 'fields', 'machines'] as const

function collect(map: Map<string, string>, entries: Array<[readonly unknown[], unknown]>) {
  for (const [, data] of entries) {
    if (!Array.isArray(data)) continue
    for (const row of data as Named[]) {
      if (row && typeof row.id === 'string' && typeof row.name === 'string' && row.name.trim() !== '') {
        if (!map.has(row.id)) map.set(row.id, row.name)
      }
    }
  }
}

/**
 * Builds an id → display-name map from already-loaded farms / fields / machines query caches.
 * Nothing is hardcoded: ids that are not in any cache fall back to a shortened id.
 */
export function buildEntityNameMap(entries: {
  farms: Array<[readonly unknown[], Farm[] | undefined]>
  fields: Array<[readonly unknown[], Field[] | undefined]>
  machines: Array<[readonly unknown[], Machine[] | undefined]>
}): Map<string, string> {
  const map = new Map<string, string>()
  collect(map, entries.farms)
  collect(map, entries.fields)
  collect(map, entries.machines)
  return map
}

export function shortEntityId(id: string): string {
  return isUuid(id) ? id.slice(0, 8) : id
}

/** Cheap fingerprint of the relevant caches; changes whenever one of them receives new data. */
function cacheFingerprint(client: QueryClient): string {
  const cache = client.getQueryCache()
  return ROOT_KEYS.map((root) =>
    cache
      .findAll({ queryKey: [root] })
      .map((q) => `${q.queryHash}:${q.state.dataUpdatedAt}`)
      .join('|'),
  ).join('||')
}

export function useEntityNames() {
  const client = useQueryClient()
  const subscribe = useCallback(
    (onChange: () => void) => client.getQueryCache().subscribe(onChange),
    [client],
  )
  const fingerprint = useSyncExternalStore(subscribe, () => cacheFingerprint(client), () => cacheFingerprint(client))

  const map = useMemo(
    () =>
      buildEntityNameMap({
        farms: client.getQueriesData<Farm[]>({ queryKey: ['farms'] }),
        fields: client.getQueriesData<Field[]>({ queryKey: ['fields'] }),
        machines: client.getQueriesData<Machine[]>({ queryKey: ['machines'] }),
      }),
    // fingerprint is the reactive dependency; client is stable.
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [client, fingerprint],
  )

  const nameOf = useCallback((id?: string | null): string | undefined => (id ? map.get(id) : undefined), [map])

  const nameOrShort = useCallback(
    (id?: string | null): string | undefined => (id ? (map.get(id) ?? shortEntityId(id)) : undefined),
    [map],
  )

  return { nameOf, nameOrShort, map }
}
