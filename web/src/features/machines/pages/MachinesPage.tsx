import { machinePhoto } from '@/shared/demo/media'
import { useMachinesQuery } from '@/features/machines/queries'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { queryError } from '@/shared/lib/queryError'
import { Card } from '@/shared/ui/Card'
import { EntityPhoto } from '@/shared/ui/EntityPhoto'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'
import { StatusBadge } from '@/shared/ui/StatusBadge'
import { useUiStore } from '@/shared/ui/uiStore'

export function MachinesPage() {
  const farmId = useUiStore((s) => s.farmId)
  const machines = useMachinesQuery(farmId)
  const err = queryError(machines.error)
  const { t } = useI18n()
  const { label } = useFormat()

  return (
    <section>
      <PageHeader title={t('machines.title')} description={t('machines.description')} />
      <QueryPageState
        isLoading={machines.isLoading}
        isError={machines.isError}
        errorMessage={err.message}
        correlationId={err.correlationId}
        isEmpty={!machines.isLoading && (machines.data?.length ?? 0) === 0}
        emptyTitle={t('machines.emptyTitle')}
        emptyDescription={t('machines.emptyDescription')}
        onRetry={() => void machines.refetch()}
      >
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {(machines.data ?? []).map((machine) => (
            <Card key={machine.id} className="flex gap-3 p-3">
              <EntityPhoto
                src={machinePhoto(machine.id, machine.type)}
                alt={label(machine.id, machine.name)}
              />
              <div className="flex min-w-0 flex-1 flex-col gap-2">
                <div className="flex items-start justify-between gap-2">
                  <h2 className="font-semibold text-pf-green">{label(machine.id, machine.name)}</h2>
                  <StatusBadge value={machine.status} />
                </div>
                <p className="text-sm text-pf-muted">
                  {label(machine.type)} · {machine.manufacturer} {machine.model}
                </p>
              </div>
            </Card>
          ))}
        </div>
      </QueryPageState>
    </section>
  )
}
