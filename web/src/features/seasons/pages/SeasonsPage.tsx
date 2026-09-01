import { useSeasonsQuery } from '@/features/seasons/queries'
import { useI18n } from '@/shared/i18n/useI18n'
import { formatDate } from '@/shared/lib/format'
import { queryError } from '@/shared/lib/queryError'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'
import { useUiStore } from '@/shared/ui/uiStore'

export function SeasonsPage() {
  const farmId = useUiStore((s) => s.farmId)
  const seasons = useSeasonsQuery(farmId)
  const err = queryError(seasons.error)
  const { t } = useI18n()

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
        <div className="overflow-hidden rounded-[12px] border border-pf-border bg-white">
          <table className="w-full text-left text-sm">
            <thead className="bg-pf-bg text-pf-muted">
              <tr>
                <th className="px-4 py-3 font-medium">{t('seasons.col.name')}</th>
                <th className="px-4 py-3 font-medium">{t('seasons.col.crop')}</th>
                <th className="px-4 py-3 font-medium">{t('seasons.col.status')}</th>
                <th className="px-4 py-3 font-medium">{t('seasons.col.period')}</th>
              </tr>
            </thead>
            <tbody>
              {(seasons.data ?? []).map((season) => (
                <tr key={season.id} className="border-t border-pf-border">
                  <td className="px-4 py-3 font-medium text-pf-green">{season.name ?? season.id}</td>
                  <td className="px-4 py-3">{season.crop ?? '—'}</td>
                  <td className="px-4 py-3">{season.status ?? '—'}</td>
                  <td className="px-4 py-3">
                    {formatDate(season.startDate)} – {formatDate(season.endDate)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </QueryPageState>
    </section>
  )
}
