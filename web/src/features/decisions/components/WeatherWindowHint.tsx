import { useWeatherWindowsQuery } from '@/features/weather/queries'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { Card } from '@/shared/ui/Card'
import { InspectorQueryState } from '@/shared/ui/InspectorQueryState'
import { StatusBadge } from '@/shared/ui/StatusBadge'

type Props = { farmId?: string | null }

/** Surfaces existing weather windows for the farm — no extra write API. */
export function WeatherWindowHint({ farmId }: Props) {
  const { t } = useI18n()
  const { dateTime, label } = useFormat()
  const query = useWeatherWindowsQuery(farmId, { enabled: Boolean(farmId) })
  const windows = query.data ?? []

  return (
    <Card className="flex flex-col gap-2">
      <h3 className="font-display text-base font-bold">{t('decisions.weatherWindow.title')}</h3>
      {!farmId ? (
        <p className="text-sm text-ag-n-600" role="status">
          {t('decisions.weatherWindow.empty')}
        </p>
      ) : (
        <InspectorQueryState
          isLoading={query.isLoading}
          error={query.error}
          onRetry={() => void query.refetch()}
        >
          {windows.length === 0 ? (
            <p className="text-sm text-ag-n-600" role="status">
              {t('decisions.weatherWindow.empty')}
            </p>
          ) : (
            <ul className="flex flex-col gap-2">
              {windows.slice(0, 3).map((row) => (
                <li key={row.id} className="flex flex-wrap items-center gap-2 text-[13px]">
                  <StatusBadge value={row.rating} />
                  <span className="font-medium">{label(row.windowType)}</span>
                  <span className="text-xs text-ag-n-600">
                    {dateTime(row.startAt)} → {dateTime(row.endAt)}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </InspectorQueryState>
      )}
    </Card>
  )
}
