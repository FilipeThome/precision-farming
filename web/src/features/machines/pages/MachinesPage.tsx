import { useMachinesQuery } from '@/features/machines/queries'
import { queryError } from '@/shared/lib/queryError'
import { Card } from '@/shared/ui/Card'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'
import { StatusBadge } from '@/shared/ui/StatusBadge'
import { useUiStore } from '@/shared/ui/uiStore'

export function MachinesPage() {
  const farmId = useUiStore((s) => s.farmId)
  const machines = useMachinesQuery(farmId)
  const err = queryError(machines.error)

  return (
    <section>
      <PageHeader title="Máquinas" description="Frota e status operacional." />
      <QueryPageState
        isLoading={machines.isLoading}
        isError={machines.isError}
        errorMessage={err.message}
        correlationId={err.correlationId}
        isEmpty={!machines.isLoading && (machines.data?.length ?? 0) === 0}
        emptyTitle="Nenhuma máquina encontrada"
        emptyDescription="Nenhum ativo retornado pelo serviço de máquinas."
        onRetry={() => void machines.refetch()}
      >
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {(machines.data ?? []).map((machine) => (
            <Card key={machine.id} className="flex flex-col gap-2">
              <div className="flex items-start justify-between gap-2">
                <h2 className="font-semibold text-pf-green">{machine.name}</h2>
                <StatusBadge value={machine.status} />
              </div>
              <p className="text-sm text-pf-muted">
                {machine.type} · {machine.manufacturer} {machine.model}
              </p>
            </Card>
          ))}
        </div>
      </QueryPageState>
    </section>
  )
}
