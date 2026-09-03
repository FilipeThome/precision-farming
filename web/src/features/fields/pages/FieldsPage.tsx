import { fieldPhoto } from '@/shared/demo/media'
import { useFieldsQuery } from '@/features/fields/queries'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { queryError } from '@/shared/lib/queryError'
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
              imageSrc={fieldPhoto(field.id, field.crop, field.farmId)}
              imageAlt={label(field.id, field.name)}
            />
          ))}
        </div>
      </QueryPageState>
    </section>
  )
}
