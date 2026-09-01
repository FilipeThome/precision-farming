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
  const err = queryError(fields.error)
  const { t } = useI18n()

  return (
    <section className="flex h-full flex-col">
      <PageHeader title={t('map.title')} description={t('map.description')} />
      <QueryPageState
        isLoading={fields.isLoading}
        isError={fields.isError}
        errorMessage={err.message}
        correlationId={err.correlationId}
        isEmpty={false}
        emptyTitle={t('fields.emptyTitle')}
        onRetry={() => void fields.refetch()}
      >
        <FieldMap fields={fields.data ?? []} className="min-h-[560px] flex-1" />
      </QueryPageState>
    </section>
  )
}
