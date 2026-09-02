import { useMachineRiskQuery } from '@/features/ai/queries'
import { useMachinesQuery } from '@/features/machines/queries'
import type { Machine } from '@/shared/api/types'
import { machinePhoto } from '@/shared/demo/media'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { queryError } from '@/shared/lib/queryError'
import { Card } from '@/shared/ui/Card'
import { EntityPhoto } from '@/shared/ui/EntityPhoto'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'
import { StatusBadge } from '@/shared/ui/StatusBadge'
import { useUiStore } from '@/shared/ui/uiStore'

function MaintenanceCard({ machine }: { machine: Machine }) {
  const risk = useMachineRiskQuery(machine.id, true)
  const first = risk.data?.[0]
  const { t } = useI18n()
  const { label, percent, dateTime } = useFormat()

  return (
    <Card className="flex gap-3 p-3">
      <EntityPhoto src={machinePhoto(machine.id, machine.type)} alt={label(machine.id, machine.name)} />
      <div className="flex min-w-0 flex-1 flex-col gap-2">
        <div className="flex items-start justify-between gap-2">
          <h2 className="font-semibold text-pf-green">{label(machine.id, machine.name)}</h2>
          <StatusBadge value={machine.status} />
        </div>
        <p className="text-sm text-pf-muted">
          {machine.manufacturer} {machine.model}
        </p>
        {risk.isLoading ? <p className="text-xs text-pf-muted">{t('common.loading')}</p> : null}
        {first ? (
          <p className="text-xs text-pf-muted">
            {t('maintenance.risk', {
              model: first.model,
              version: first.modelVersion,
              score: percent(first.score),
              at: dateTime(first.generatedAt),
            })}
          </p>
        ) : null}
      </div>
    </Card>
  )
}

export function MaintenancePage() {
  const farmId = useUiStore((s) => s.farmId)
  const machines = useMachinesQuery(farmId)
  const err = queryError(machines.error)
  const { t } = useI18n()
  const items = (machines.data ?? []).filter((m) => m.status === 'MAINTENANCE')

  return (
    <section>
      <PageHeader title={t('maintenance.title')} description={t('maintenance.description')} />
      <QueryPageState
        isLoading={machines.isLoading}
        isError={machines.isError}
        errorMessage={err.message}
        correlationId={err.correlationId}
        isEmpty={!machines.isLoading && items.length === 0}
        emptyTitle={t('maintenance.emptyTitle')}
        emptyDescription={t('maintenance.emptyDescription')}
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
