import { Bell, LandPlot, ListChecks, Tractor } from 'lucide-react'

import { useAlertsQuery } from '@/features/alerts/queries'
import { useFarmsQuery } from '@/features/farms/queries'
import { useMachinesQuery } from '@/features/machines/queries'
import { useOperationsQuery } from '@/features/operations/queries'
import { queryError } from '@/shared/lib/queryError'
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

  const loading = farms.isLoading || machines.isLoading || operations.isLoading || alerts.isLoading
  const error = farms.error || machines.error || operations.error || alerts.error
  const err = queryError(error)

  const farmCount = farms.data?.length ?? 0
  const machineCount = machines.data?.length ?? 0
  const operationCount = operations.data?.length ?? 0
  const alertCount = alerts.data?.length ?? 0
  const empty = !loading && !error && farmCount + machineCount + operationCount + alertCount === 0

  return (
    <section>
      <PageHeader title="Dashboard" description="Resumo operacional das fazendas." />
      <QueryPageState
        isLoading={loading}
        isError={Boolean(error)}
        errorMessage={err.message}
        correlationId={err.correlationId}
        isEmpty={empty}
        emptyTitle="Nenhum dado disponível"
        emptyDescription="Faça o seed do backend ou cadastre fazendas para começar."
        onRetry={() => {
          void farms.refetch()
          void machines.refetch()
          void operations.refetch()
          void alerts.refetch()
        }}
      >
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <KpiCard label="Fazendas" value={farmCount} icon={LandPlot} />
          <KpiCard label="Máquinas" value={machineCount} icon={Tractor} />
          <KpiCard label="Operações" value={operationCount} icon={ListChecks} />
          <KpiCard label="Alertas" value={alertCount} icon={Bell} />
        </div>
      </QueryPageState>
    </section>
  )
}
