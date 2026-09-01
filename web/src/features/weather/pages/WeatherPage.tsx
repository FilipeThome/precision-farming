import { useState } from 'react'

import { WeatherWindowsList } from '@/features/weather/components/WeatherWindowsList'
import { useForecastQuery, useWeatherWindowsQuery } from '@/features/weather/queries'
import { weatherSeries } from '@/shared/charts/adapters'
import { weatherPhoto } from '@/shared/demo/media'
import { useI18n } from '@/shared/i18n/useI18n'
import { formatDate, formatNumber } from '@/shared/lib/format'
import { queryError } from '@/shared/lib/queryError'
import { Card } from '@/shared/ui/Card'
import { CHART_COLORS, ChartCard } from '@/shared/ui/ChartCard'
import { LineChartBlock } from '@/shared/ui/charts'
import { EntityPhoto } from '@/shared/ui/EntityPhoto'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'
import { SectionTabs } from '@/shared/ui/SectionTabs'
import { StatusBadge } from '@/shared/ui/StatusBadge'
import { useUiStore } from '@/shared/ui/uiStore'

type Tab = 'forecast' | 'windows'

export function WeatherPage() {
  const farmId = useUiStore((s) => s.farmId)
  const [tab, setTab] = useState<Tab>('forecast')
  const forecast = useForecastQuery(farmId, { enabled: tab === 'forecast' })
  const windows = useWeatherWindowsQuery(farmId, { enabled: tab === 'windows' })
  const active = tab === 'forecast' ? forecast : windows
  const err = queryError(active.error)
  const { t } = useI18n()

  return (
    <section>
      <PageHeader title={t('weather.title')} description={t('weather.description')} />
      <SectionTabs
        active={tab}
        onChange={setTab}
        tabs={[
          { id: 'forecast', labelKey: 'weather.tab.forecast' },
          { id: 'windows', labelKey: 'weather.tab.windows' },
        ]}
      />
      <QueryPageState
        isLoading={active.isLoading}
        isError={active.isError}
        errorMessage={err.message}
        correlationId={err.correlationId}
        isEmpty={!active.isLoading && (active.data?.length ?? 0) === 0}
        emptyTitle={tab === 'forecast' ? t('weather.emptyTitle') : t('weather.windows.emptyTitle')}
        emptyDescription={
          tab === 'forecast' ? t('weather.emptyDescription') : t('weather.windows.emptyDescription')
        }
        onRetry={() => void active.refetch()}
      >
        {tab === 'windows' ? (
          <WeatherWindowsList items={windows.data ?? []} />
        ) : (
          <>
          {(forecast.data?.length ?? 0) > 0 ? (
            <ChartCard title={t('charts.weatherForecast')} className="mb-4">
              <LineChartBlock
                data={weatherSeries(forecast.data ?? [])}
                xKey="name"
                lines={[
                  { dataKey: 'tMin', name: t('charts.tempMin'), color: CHART_COLORS.blue },
                  { dataKey: 'tMax', name: t('charts.tempMax'), color: CHART_COLORS.amber },
                  { dataKey: 'rain', name: t('charts.rain'), color: CHART_COLORS.teal },
                ]}
              />
            </ChartCard>
          ) : null}
          <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
            {(forecast.data ?? []).map((day) => (
              <Card key={day.id} className="overflow-hidden p-0">
                <EntityPhoto src={weatherPhoto()} alt={formatDate(day.forecastAt)} />
                <div className="p-4">
                  <div className="flex items-start justify-between gap-2">
                    <h2 className="font-medium text-pf-green">{formatDate(day.forecastAt)}</h2>
                    <StatusBadge value={day.sprayingWindow} />
                  </div>
                  <p className="mt-2 text-sm">
                    {formatNumber(Number(day.temperatureMin), 0)}–
                    {formatNumber(Number(day.temperatureMax), 0)} °C
                  </p>
                  <p className="text-sm text-pf-muted">
                    {t('weather.rainWind', {
                      rain: formatNumber(Number(day.rainMm), 1),
                      wind: formatNumber(Number(day.windKmh), 0),
                    })}
                  </p>
                </div>
              </Card>
            ))}
          </div>
          </>
        )}
      </QueryPageState>
    </section>
  )
}
