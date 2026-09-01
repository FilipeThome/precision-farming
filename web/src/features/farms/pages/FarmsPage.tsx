import { useFarmsQuery } from '@/features/farms/queries'
import { useI18n } from '@/shared/i18n/useI18n'
import { formatNumber } from '@/shared/lib/format'
import { queryError } from '@/shared/lib/queryError'
import { Card } from '@/shared/ui/Card'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'

export function FarmsPage() {
  const farms = useFarmsQuery()
  const err = queryError(farms.error)
  const { t } = useI18n()

  return (
    <section>
      <PageHeader title={t('farms.title')} description={t('farms.description')} />
      <QueryPageState
        isLoading={farms.isLoading}
        isError={farms.isError}
        errorMessage={err.message}
        correlationId={err.correlationId}
        isEmpty={!farms.isLoading && (farms.data?.length ?? 0) === 0}
        emptyTitle={t('farms.emptyTitle')}
        emptyDescription={t('farms.emptyDescription')}
        onRetry={() => void farms.refetch()}
      >
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {(farms.data ?? []).map((farm) => (
            <Card key={farm.id}>
              <h2 className="text-lg font-semibold text-pf-green">{farm.name}</h2>
              <p className="mt-1 text-sm text-pf-muted">{farm.location}</p>
              <dl className="mt-3 grid grid-cols-2 gap-2 text-sm">
                <div>
                  <dt className="text-pf-muted">Área</dt>
                  <dd>{formatNumber(Number(farm.areaHa), 1)} ha</dd>
                </div>
                <div>
                  <dt className="text-pf-muted">Fuso</dt>
                  <dd>{farm.timezone}</dd>
                </div>
              </dl>
            </Card>
          ))}
        </div>
      </QueryPageState>
    </section>
  )
}
