import { cropPhoto, farmPhoto } from '@/shared/demo/media'
import { useSeasonsQuery } from '@/features/seasons/queries'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { queryError } from '@/shared/lib/queryError'
import { EntityCard } from '@/shared/ui/EntityCard'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'
import { StatusBadge } from '@/shared/ui/StatusBadge'
import { useUiStore } from '@/shared/ui/uiStore'

export function SeasonsPage() {
  const farmId = useUiStore((s) => s.farmId)
  const seasons = useSeasonsQuery(farmId)
  const err = queryError(seasons.error)
  const { t } = useI18n()
  const { date, label } = useFormat()

  return (
    <section>
      <PageHeader title={t('seasons.title')} description={t('seasons.description')} />
      <QueryPageState
        isLoading={seasons.isLoading}
        isError={seasons.isError}
        errorMessage={err.message}
        correlationId={err.correlationId}
        isEmpty={!seasons.isLoading && (seasons.data?.length ?? 0) === 0}
        emptyTitle={t('seasons.emptyTitle')}
        emptyDescription={t('seasons.emptyDescription')}
        onRetry={() => void seasons.refetch()}
      >
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {(seasons.data ?? []).map((season) => (
            <EntityCard
              key={season.id}
              title={label(season.name, season.id)}
              subtitle={season.crop ? label(season.crop) : undefined}
              meta={`${date(season.startDate)} – ${date(season.endDate)}`}
              imageSrc={season.crop ? cropPhoto(season.crop) : farmPhoto(season.farmId)}
              imageAlt={label(season.name, season.crop)}
            >
              {season.status ? <StatusBadge value={season.status} /> : null}
            </EntityCard>
          ))}
        </div>
      </QueryPageState>
    </section>
  )
}
