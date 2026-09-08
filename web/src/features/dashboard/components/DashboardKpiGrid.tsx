import { AlertTriangle, Gauge, LandPlot, ListChecks, Percent, Tractor, TrendingDown } from 'lucide-react'

import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { KpiCard } from '@/shared/ui/KpiCard'

type Props = {
  farmCount: number
  machineCount: number
  operationCount: number
  alertCount: number
  opsProgressPct: number
  completedOps: number
  criticalAlerts: number
  fleetPct: number
  negativeMargins: number
}

export function DashboardKpiGrid({
  farmCount,
  machineCount,
  operationCount,
  alertCount,
  opsProgressPct,
  completedOps,
  criticalAlerts,
  fleetPct,
  negativeMargins,
}: Props) {
  const { t } = useI18n()
  const { number } = useFormat()

  return (
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <KpiCard label={t('dashboard.kpi.farms')} value={farmCount} icon={LandPlot} to="/farms" />
      <KpiCard label={t('dashboard.kpi.machines')} value={machineCount} icon={Tractor} to="/machines" />
      <KpiCard label={t('dashboard.kpi.operations')} value={operationCount} icon={ListChecks} to="/operations" />
      <KpiCard label={t('dashboard.kpi.alerts')} value={alertCount} icon={AlertTriangle} to="/alerts" />
      <KpiCard
        label={t('dashboard.kpi.opsProgress')}
        value={`${opsProgressPct}%`}
        icon={Gauge}
        hint={t('dashboard.kpi.opsProgressHint', { completed: completedOps, total: operationCount })}
        to="/operations"
      />
      <KpiCard
        label={t('dashboard.kpi.criticalAlerts')}
        value={criticalAlerts}
        icon={AlertTriangle}
        hint={t('dashboard.kpi.criticalAlertsHint')}
        to="/alerts?severity=CRITICAL"
      />
      <KpiCard
        label={t('dashboard.kpi.fleetAvailability')}
        value={`${fleetPct}%`}
        icon={Percent}
        hint={t('dashboard.kpi.fleetAvailabilityHint')}
        to="/machines"
      />
      <KpiCard
        label={t('dashboard.kpi.marginRisk')}
        value={number(negativeMargins, 0)}
        icon={TrendingDown}
        hint={t('dashboard.kpi.marginRiskHint')}
        to="/finance"
      />
    </div>
  )
}
