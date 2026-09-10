import type { Alert, FinancePnl, Machine, Operation } from '@/shared/api/types'
import { alertSeverityPie, fleetStatusBars, opsStatusBars, pnlChartRows } from '@/shared/charts/adapters'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { CHART_COLORS, ChartCard } from '@/shared/ui/ChartCard'
import { BarChartBlock, PieChartBlock } from '@/shared/ui/charts'

type Props = {
  operations: Operation[]
  alerts: Alert[]
  machines: Machine[]
  pnl: FinancePnl[]
}

export function DashboardCharts({ operations, alerts, machines, pnl }: Props) {
  const { t } = useI18n()
  const { label } = useFormat()

  return (
    <div className="mt-6 grid gap-4 lg:grid-cols-2 xl:grid-cols-3">
      <ChartCard title={t('charts.opsByStatus')} description={t('charts.fromLive')} to="/operations">
        <BarChartBlock
          data={opsStatusBars(operations, label)}
          xKey="name"
          bars={[{ dataKey: 'value', name: t('charts.count'), color: CHART_COLORS.green }]}
        />
      </ChartCard>
      <ChartCard title={t('charts.alertsBySeverity')} description={t('charts.fromLive')} to="/alerts">
        <PieChartBlock data={alertSeverityPie(alerts, label)} />
      </ChartCard>
      <ChartCard title={t('charts.fleetByStatus')} description={t('charts.fromLive')} to="/machines">
        <BarChartBlock
          data={fleetStatusBars(machines, label)}
          xKey="name"
          bars={[{ dataKey: 'value', name: t('charts.count'), color: CHART_COLORS.teal }]}
        />
      </ChartCard>
      <ChartCard
        title={t('charts.pnlSummary')}
        description={t('charts.pnlHint')}
        className="xl:col-span-3"
        to="/finance"
      >
        <BarChartBlock
          data={pnlChartRows(pnl, label)}
          xKey="name"
          bars={[
            { dataKey: 'revenue', name: t('charts.revenue'), color: CHART_COLORS.green },
            { dataKey: 'cost', name: t('charts.cost'), color: CHART_COLORS.amber },
            { dataKey: 'margin', name: t('charts.margin'), color: CHART_COLORS.teal },
          ]}
        />
      </ChartCard>
    </div>
  )
}
