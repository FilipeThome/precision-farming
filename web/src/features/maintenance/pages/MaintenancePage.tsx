import { useMachineRiskQuery } from '@/features/ai/queries'
import { useMachinesQuery } from '@/features/machines/queries'
import { formatDateTime, formatPercent } from '@/shared/lib/format'
import { queryError } from '@/shared/lib/queryError'
import type { Machine } from '@/shared/api/types'
import { Card } from '@/shared/ui/Card'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'
import { StatusBadge } from '@/shared/ui/StatusBadge'
import { useUiStore } from '@/shared/ui/uiStore'

function MaintenanceCard({ machine }: { machine: Machine }) {
  const risk = useMachineRiskQuery(machine.id, true)
  const first = risk.data?.[0]

  return (
    <Card className="flex flex-col gap-2">
      <div className="flex items-start justify-between gap-2">
        <h2 className="font-semibold text-pf-green">{machine.name}</h2>
        <StatusBadge value={machine.status} />
      </div>
      <p className="text-sm text-pf-muted">
        {machine.manufacturer} {machine.model}
      </p>
      {risk.isLoading ? <p className="text-xs text-pf-muted">Carregando risco…</p> : null}
      {first ? (
        <p className="text-xs text-pf-muted">
          Risco (modelo {first.model} {first.modelVersion}): {formatPercent(first.score)} · gerado em{' '}
          {formatDateTime(first.generatedAt)}
        </p>
      ) : null}
    </Card>
  )
}

export function MaintenancePage() {
  const farmId = useUiStore((s) => s.farmId)
  const machines = useMachinesQuery(farmId)
  const err = queryError(machines.error)
  const items = (machines.data ?? []).filter((m) => m.status === 'MAINTENANCE')

  return (
    <section>
      <PageHeader
        title="Manutenção"
        description="Máquinas com status de manutenção e risco estimado pelo serviço de IA."
      />
      <QueryPageState
        isLoading={machines.isLoading}
        isError={machines.isError}
        errorMessage={err.message}
        correlationId={err.correlationId}
        isEmpty={!machines.isLoading && items.length === 0}
        emptyTitle="Nenhuma máquina em manutenção"
        emptyDescription="Quando uma máquina estiver com status MAINTENANCE, ela aparece aqui."
        onRetry={() => void machines.refetch()}
      >
        <div className="grid gap-4 md:grid-cols-2">
          {items.map((machine) => (
            <MaintenanceCard key={machine.id} machine={machine} />
          ))}
        </div>
      </QueryPageState>
    </section>
  )
}
