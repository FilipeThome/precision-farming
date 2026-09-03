import { useEffect, useMemo, useState } from 'react'

import { MapLayerToggles } from '@/features/map/components/MapLayerToggles'
import { useMapLayersQuery } from '@/features/map/queries'
import { useFieldsQuery } from '@/features/fields/queries'
import { useI18n } from '@/shared/i18n/useI18n'
import { queryError } from '@/shared/lib/queryError'
import { FieldMap } from '@/shared/maps/FieldMap'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'
import { useUiStore } from '@/shared/ui/uiStore'

export function MapPage() {
  const farmId = useUiStore((s) => s.farmId)
  const fields = useFieldsQuery(farmId)
  const layers = useMapLayersQuery(farmId)
  const [enabledKinds, setEnabledKinds] = useState<Set<string>>(new Set())
  const err = queryError(fields.error || layers.error)
  const { t } = useI18n()

  useEffect(() => {
    const kinds = layers.data?.map((layer) => layer.kind) ?? []
    if (kinds.length === 0) return
    setEnabledKinds((prev) => (prev.size === 0 ? new Set(kinds) : prev))
  }, [layers.data])

  function toggleKind(kind: string) {
    setEnabledKinds((prev) => {
      const next = new Set(prev)
      if (next.has(kind)) next.delete(kind)
      else next.add(kind)
      return next
    })
  }

  const activeLayerKinds = useMemo(() => [...enabledKinds], [enabledKinds])

  const loading = fields.isLoading || layers.isLoading
  const isError = fields.isError || layers.isError

  return (
    <section className="flex h-full flex-col">
      <PageHeader title={t('map.title')} description={t('map.description')} />
      <QueryPageState
        isLoading={loading}
        isError={isError}
        errorMessage={err.message}
        correlationId={err.correlationId}
        isEmpty={false}
        emptyTitle={t('fields.emptyTitle')}
        onRetry={() => {
          void fields.refetch()
          void layers.refetch()
        }}
      >
        <MapLayerToggles
          layers={layers.data ?? []}
          enabledKinds={enabledKinds}
          onToggle={toggleKind}
        />
        <FieldMap
          fields={fields.data ?? []}
          layers={layers.data ?? []}
          activeLayerKinds={activeLayerKinds}
          className="min-h-[560px] flex-1"
        />
      </QueryPageState>
    </section>
  )
}
