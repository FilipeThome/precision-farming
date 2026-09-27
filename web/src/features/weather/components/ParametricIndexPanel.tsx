import { useParametricIndexQuery } from '@/features/weather/queries'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { queryError } from '@/shared/lib/queryError'
import { Card } from '@/shared/ui/Card'
import { QueryPageState } from '@/shared/ui/QueryPageState'
import { SimulationNotice } from '@/shared/ui/SimulationNotice'
import { StatusBadge } from '@/shared/ui/StatusBadge'

type Props = { farmId?: string | null }

export function ParametricIndexPanel({ farmId }: Props) {
  const { t } = useI18n()
  const { date, number } = useFormat()
  const query = useParametricIndexQuery(farmId)
  const err = queryError(query.error)
  const days = query.data?.days ?? []

  if (!farmId) {
    return (
      <QueryPageState
        isLoading={false}
        isError={false}
        isEmpty
        emptyTitle={t('weather.index.emptyTitle')}
        emptyDescription={t('weather.index.emptyDescription')}
      >
        {null}
      </QueryPageState>
    )
  }

  return (
    <QueryPageState
      isLoading={query.isLoading}
      isError={query.isError}
      errorMessage={err.message}
      correlationId={err.correlationId}
      isEmpty={!query.isLoading && days.length === 0}
      emptyTitle={t('weather.index.emptyTitle')}
      emptyDescription={t('weather.index.emptyDescription')}
      onRetry={() => void query.refetch()}
    >
      <Card className="flex flex-col gap-3 overflow-x-auto">
        <SimulationNotice
          simulation={query.data?.simulation === true}
          farm={query.data?.farmId ?? farmId}
        />
        <p className="text-xs text-ag-n-600">
          {t('demo.simulationFarm', { farm: query.data?.farmId ?? farmId })}
        </p>
        <table className="w-full min-w-[28rem] border-collapse text-left text-sm">
          <thead>
            <tr className="border-b border-ag-n-200 text-[11px] uppercase tracking-[0.04em] text-ag-n-500">
              <th className="py-2 pr-3 font-semibold"> </th>
              <th className="py-2 pr-3 font-semibold">{t('weather.index.rain')}</th>
              <th className="py-2 pr-3 font-semibold">{t('weather.index.deficit')}</th>
              <th className="py-2 font-semibold">{t('weather.index.frost')}</th>
            </tr>
          </thead>
          <tbody>
            {days.map((row) => (
              <tr key={row.date} className="border-b border-ag-n-100 last:border-b-0">
                <td className="py-2 pr-3 font-mono">{date(row.date)}</td>
                <td className="py-2 pr-3 font-mono">{number(Number(row.rainMm), 1)} mm</td>
                <td className="py-2 pr-3 font-mono">{number(Number(row.waterDeficitMm), 1)} mm</td>
                <td className="py-2">
                  <StatusBadge
                    value={
                      row.frostRisk === true || row.frostRisk === 'true' || row.frostRisk === 1
                        ? 'RISK'
                        : 'OK'
                    }
                  />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
    </QueryPageState>
  )
}
