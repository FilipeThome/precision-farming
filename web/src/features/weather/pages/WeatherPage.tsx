import { useForecastQuery } from '@/features/weather/queries'
import { formatDate, formatNumber } from '@/shared/lib/format'
import { queryError } from '@/shared/lib/queryError'
import { Card } from '@/shared/ui/Card'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'
import { StatusBadge } from '@/shared/ui/StatusBadge'
import { useUiStore } from '@/shared/ui/uiStore'

export function WeatherPage() {
  const farmId = useUiStore((s) => s.farmId)
  const forecast = useForecastQuery(farmId)
  const err = queryError(forecast.error)

  return (
    <section>
      <PageHeader title="Clima" description="Previsão e janela de pulverização." />
      <QueryPageState
        isLoading={forecast.isLoading}
        isError={forecast.isError}
        errorMessage={err.message}
        correlationId={err.correlationId}
        isEmpty={!forecast.isLoading && (forecast.data?.length ?? 0) === 0}
        emptyTitle="Sem previsão disponível"
        emptyDescription="O serviço de clima não retornou dados para esta fazenda."
        onRetry={() => void forecast.refetch()}
      >
        <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
          {(forecast.data ?? []).map((day) => (
            <Card key={day.id}>
              <div className="flex items-start justify-between gap-2">
                <h2 className="font-medium text-pf-green">{formatDate(day.forecastAt)}</h2>
                <StatusBadge value={day.sprayingWindow} />
              </div>
              <p className="mt-2 text-sm">
                {formatNumber(Number(day.temperatureMin), 0)}–{formatNumber(Number(day.temperatureMax), 0)} °C
              </p>
              <p className="text-sm text-pf-muted">
                Chuva {formatNumber(Number(day.rainMm), 1)} mm · vento {formatNumber(Number(day.windKmh), 0)} km/h
              </p>
            </Card>
          ))}
        </div>
      </QueryPageState>
    </section>
  )
}
