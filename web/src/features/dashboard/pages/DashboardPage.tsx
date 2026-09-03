import { AlertTriangle, Gauge, LandPlot, ListChecks, Percent, Tractor, TrendingDown } from 'lucide-react'

import { useAlertsQuery } from '@/features/alerts/queries'
import { useFarmsQuery } from '@/features/farms/queries'
import { useFinancePnlQuery } from '@/features/finance/queries'
import { useMachinesQuery } from '@/features/machines/queries'
import { useOperationsQuery } from '@/features/operations/queries'
import {
  alertSeverityPie,
  fleetStatusBars,
  opsStatusBars,
  pnlChartRows,
} from '@/shared/charts/adapters'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { queryError } from '@/shared/lib/queryError'
import { CHART_COLORS, ChartCard } from '@/shared/ui/ChartCard'
import { BarChartBlock, PieChartBlock } from '@/shared/ui/charts'
import { KpiCard } from '@/shared/ui/KpiCard'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'
import { useUiStore } from '@/shared/ui/uiStore'

export function DashboardPage() {
  const farmId = useUiStore((s) => s.farmId)
  const farms = useFarmsQuery()
  const machines = useMachinesQuery(farmId)
  const operations = useOperationsQuery(farmId)
  const alerts = useAlertsQuery(farmId)
  const pnl = useFinancePnlQuery(farmId)
  const { t } = useI18n()
  const { number, label } = useFormat()

  const loading =
    farms.isLoading ||
    machines.isLoading ||
    operations.isLoading ||
    alerts.isLoading ||
    pnl.isLoading
  const error = farms.error || machines.error || operations.error || alerts.error || pnl.error
  const err = queryError(error)

  const farmCount = farms.data?.length ?? 0
  const machineCount = machines.data?.length ?? 0
  const operationCount = operations.data?.length ?? 0
  const alertCount = alerts.data?.length ?? 0

  const completedOps = (operations.data ?? []).filter((op) => op.status === 'COMPLETED').length
  const opsProgressPct =
    operationCount === 0 ? 0 : Math.round((completedOps / operationCount) * 100)

  const criticalAlerts = (alerts.data ?? []).filter(
    (alert) => alert.severity === 'CRITICAL' && alert.status === 'OPEN',
  ).length

  const availableMachines = (machines.data ?? []).filter(
    (machine) => machine.status === 'OPERATING' || machine.status === 'IDLE',
  ).length
  const fleetPct = machineCount === 0 ? 0 : Math.round((availableMachines / machineCount) * 100)

  const negativeMargins = (pnl.data ?? []).filter((row) => Number(row.margin ?? 0) < 0).length

  const empty =
    !loading &&
    !error &&
    farmCount + machineCount + operationCount + alertCount + (pnl.data?.length ?? 0) === 0

  return (
    <section>
      <PageHeader title={t('dashboard.title')} description={t('dashboard.description')} />
      <QueryPageState
        isLoading={loading}
        isError={Boolean(error)}
        errorMessage={err.message}
        correlationId={err.correlationId}
        isEmpty={empty}
        emptyTitle={t('dashboard.emptyTitle')}
        emptyDescription={t('dashboard.emptyDescription')}
        onRetry={() => {
          void farms.refetch()
          void machines.refetch()
          void operations.refetch()
          void alerts.refetch()
          void pnl.refetch()
        }}
      >
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <KpiCard label={t('dashboard.kpi.farms')} value={farmCount} icon={LandPlot} />
          <KpiCard label={t('dashboard.kpi.machines')} value={machineCount} icon={Tractor} />
          <KpiCard label={t('dashboard.kpi.operations')} value={operationCount} icon={ListChecks} />
          <KpiCard label={t('dashboard.kpi.alerts')} value={alertCount} icon={AlertTriangle} />
          <KpiCard
            label={t('dashboard.kpi.opsProgress')}
            value={`${opsProgressPct}%`}
            icon={Gauge}
            hint={t('dashboard.kpi.opsProgressHint', {
              completed: completedOps,
              total: operationCount,
            })}
          />
          <KpiCard
            label={t('dashboard.kpi.criticalAlerts')}
            value={criticalAlerts}
            icon={AlertTriangle}
            hint={t('dashboard.kpi.criticalAlertsHint')}
          />
          <KpiCard
            label={t('dashboard.kpi.fleetAvailability')}
            value={`${fleetPct}%`}
            icon={Percent}
            hint={t('dashboard.kpi.fleetAvailabilityHint')}
          />
          <KpiCard
            label={t('dashboard.kpi.marginRisk')}
            value={number(negativeMargins, 0)}
            icon={TrendingDown}
            hint={t('dashboard.kpi.marginRiskHint')}
          />
        </div>

        <div className="mt-6 grid gap-4 lg:grid-cols-2 xl:grid-cols-3">
          <ChartCard title={t('charts.opsByStatus')} description={t('charts.fromSeed')}>
            <BarChartBlock
              data={opsStatusBars(operations.data ?? [], label)}
              xKey="name"
              bars={[{ dataKey: 'value', name: t('charts.count'), color: CHART_COLORS.green }]}
            />
          </ChartCard>
          <ChartCard title={t('charts.alertsBySeverity')} description={t('charts.fromSeed')}>
            <PieChartBlock data={alertSeverityPie(alerts.data ?? [], label)} />
          </ChartCard>
          <ChartCard title={t('charts.fleetByStatus')} description={t('charts.fromSeed')}>
            <BarChartBlock
              data={fleetStatusBars(machines.data ?? [], label)}
              xKey="name"
              bars={[{ dataKey: 'value', name: t('charts.count'), color: CHART_COLORS.teal }]}
            />
          </ChartCard>
          <ChartCard
            title={t('charts.pnlSummary')}
            description={t('charts.pnlHint')}
            className="xl:col-span-3"
          >
            <BarChartBlock
              data={pnlChartRows(pnl.data ?? [], label)}
              xKey="name"
              bars={[
                { dataKey: 'revenue', name: t('charts.revenue'), color: CHART_COLORS.green },
                { dataKey: 'cost', name: t('charts.cost'), color: CHART_COLORS.amber },
                { dataKey: 'margin', name: t('charts.margin'), color: CHART_COLORS.teal },
              ]}
            />
          </ChartCard>
        </div>
      </QueryPageState>
    </section>
  )
}
