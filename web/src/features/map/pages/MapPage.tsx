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
import { Button } from '@/shared/ui/Button'
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
  const rows = fields.data ?? []
  const showFieldList = !fields.isLoading && !fields.isError && rows.length > 0

  return (
    <section className="flex h-full min-h-0 flex-col overflow-hidden">
      <PageHeader title={t('map.title')} description={t('map.description')} />
      <div className="flex min-h-0 flex-1 flex-col gap-3 lg:flex-row">
        <div className="flex min-h-0 min-w-0 flex-1 flex-col">
          <FieldStatusMap
            fields={rows}
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
        </div>
        {showFieldList ? (
          <nav aria-label={t('map.fieldList')} className="max-h-40 shrink-0 overflow-auto lg:max-h-none lg:w-60">
            <ul className="flex gap-2 lg:flex-col">
              {rows.map((field) => (
                <li key={field.id}>
                  <Button
                    variant={field.id === selectedId ? 'primary' : 'secondary'}
                    aria-pressed={field.id === selectedId}
                    className="w-full justify-start"
                    onClick={() => setSelectedId(field.id)}
                  >
                    {label(field.id, field.name)}
                  </Button>
                </li>
              ))}
            </ul>
          </nav>
        ) : null}
      </div>
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
