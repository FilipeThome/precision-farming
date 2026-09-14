import { useMemo } from 'react'

import { FieldStatusMap } from '@/features/dashboard/components/FieldStatusMap'
import { fieldStates as deriveFieldStates } from '@/features/dashboard/model/fieldStatus'
import { FieldInspector } from '@/features/fields/components/FieldInspector'
import { useFieldsQuery } from '@/features/fields/queries'
import { useOperationsQuery } from '@/features/operations/queries'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { queryError } from '@/shared/lib/queryError'
import { useSelectedId } from '@/shared/lib/useSelectedId'
import { DetailDrawer } from '@/shared/ui/DetailDrawer'
import { PageHeader } from '@/shared/ui/PageHeader'
import { useUiStore } from '@/shared/ui/uiStore'

export function MapPage() {
  const farmId = useUiStore((s) => s.farmId)
  const fields = useFieldsQuery(farmId)
  const operations = useOperationsQuery(farmId)
  const { t } = useI18n()
  const { label } = useFormat()
  const { selectedId, setSelectedId } = useSelectedId()
  const selected = (fields.data ?? []).find((field) => field.id === selectedId)
  const loadError = queryError(fields.error ?? operations.error)

  const states = useMemo(
    () => deriveFieldStates(fields.data ?? [], operations.data ?? []),
    [fields.data, operations.data],
  )

  return (
    <section className="flex h-full min-h-0 flex-col overflow-hidden">
      <PageHeader title={t('map.title')} description={t('map.description')} />
      <FieldStatusMap
        fields={fields.data ?? []}
        fieldStates={states}
        isLoading={fields.isLoading || operations.isLoading}
        isError={fields.isError || operations.isError}
        errorMessage={loadError.message}
        correlationId={loadError.correlationId}
        onRetry={() => {
          void fields.refetch()
          void operations.refetch()
        }}
        onFieldClick={setSelectedId}
        minHeightClass="min-h-[320px] h-full"
      />
      <DetailDrawer
        open={Boolean(selectedId)}
        title={selected ? label(selected.id, selected.name) : t('inspector.notFound')}
        subtitle={selected ? undefined : t('inspector.notFoundHint')}
        onClose={() => setSelectedId(null)}
      >
        {selected ? <FieldInspector field={selected} /> : null}
      </DetailDrawer>
    </section>
  )
}
