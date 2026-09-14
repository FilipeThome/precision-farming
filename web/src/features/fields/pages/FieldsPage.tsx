import { FieldInspector } from '@/features/fields/components/FieldInspector'
import { useFieldsQuery } from '@/features/fields/queries'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { queryError } from '@/shared/lib/queryError'
import { useSelectedId } from '@/shared/lib/useSelectedId'
import { DetailDrawer } from '@/shared/ui/DetailDrawer'
import { EntityCard } from '@/shared/ui/EntityCard'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'
import { useUiStore } from '@/shared/ui/uiStore'

export function FieldsPage() {
  const farmId = useUiStore((s) => s.farmId)
  const fields = useFieldsQuery(farmId)
  const err = queryError(fields.error)
  const { t } = useI18n()
  const { number, label } = useFormat()
  const { selectedId, setSelectedId } = useSelectedId()
  const selected = (fields.data ?? []).find((field) => field.id === selectedId)

  return (
    <section>
      <PageHeader title={t('fields.title')} description={t('fields.description')} />
      <QueryPageState
        isLoading={fields.isLoading}
        isError={fields.isError}
        errorMessage={err.message}
        correlationId={err.correlationId}
        isEmpty={!fields.isLoading && (fields.data?.length ?? 0) === 0}
        emptyTitle={t('fields.emptyTitle')}
        emptyDescription={t('fields.emptyDescription')}
        onRetry={() => void fields.refetch()}
      >
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {(fields.data ?? []).map((field) => (
            <EntityCard
              key={field.id}
              title={label(field.id, field.name)}
              subtitle={`${label(field.crop)}${field.variety ? ` · ${label(field.variety)}` : ''}`}
              meta={`${number(Number(field.areaHa), 1)} ha`}
              kind="field"
              selected={field.id === selectedId}
              onSelect={() => setSelectedId(field.id)}
            />
          ))}
        </div>
      </QueryPageState>
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
