import { ControlTowerStrip } from '@/features/dashboard/components/ControlTowerStrip'
import { DashboardCharts } from '@/features/dashboard/components/DashboardCharts'
import { DashboardKpiGrid } from '@/features/dashboard/components/DashboardKpiGrid'
import { useAlertsQuery } from '@/features/alerts/queries'
import { useFarmsQuery } from '@/features/farms/queries'
import { useFinancePnlQuery } from '@/features/finance/queries'
import { useMachinesQuery } from '@/features/machines/queries'
import { useOperationsQuery } from '@/features/operations/queries'
import { useI18n } from '@/shared/i18n/useI18n'
import { queryError } from '@/shared/lib/queryError'
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
  const opsProgressPct = operationCount === 0 ? 0 : Math.round((completedOps / operationCount) * 100)
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
        <DashboardKpiGrid
          farmCount={farmCount}
          machineCount={machineCount}
          operationCount={operationCount}
          alertCount={alertCount}
          opsProgressPct={opsProgressPct}
          completedOps={completedOps}
          criticalAlerts={criticalAlerts}
          fleetPct={fleetPct}
          negativeMargins={negativeMargins}
        />
        <ControlTowerStrip
          alerts={alerts.data ?? []}
          machines={machines.data ?? []}
          operations={operations.data ?? []}
        />
        <DashboardCharts
          operations={operations.data ?? []}
          alerts={alerts.data ?? []}
          machines={machines.data ?? []}
          pnl={pnl.data ?? []}
        />
      </QueryPageState>
    </section>
  )
}
