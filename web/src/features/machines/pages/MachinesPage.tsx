import { MachineInspector } from '@/features/machines/components/MachineInspector'
import { useMachinesQuery } from '@/features/machines/queries'
import { machinePhoto } from '@/shared/demo/media'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { queryError } from '@/shared/lib/queryError'
import { useSelectedId } from '@/shared/lib/useSelectedId'
import { DetailDrawer } from '@/shared/ui/DetailDrawer'
import { EntityCard } from '@/shared/ui/EntityCard'
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
  const { selectedId, setSelectedId } = useSelectedId()
  const selected = (machines.data ?? []).find((machine) => machine.id === selectedId)

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
            <EntityCard
              key={machine.id}
              title={label(machine.id, machine.name)}
              subtitle={`${label(machine.type)} · ${machine.manufacturer} ${machine.model}`}
              imageSrc={machinePhoto(machine.id, machine.type)}
              imageAlt={label(machine.id, machine.name)}
              selected={machine.id === selectedId}
              onSelect={() => setSelectedId(machine.id)}
            >
              <StatusBadge value={machine.status} />
            </EntityCard>
          ))}
        </div>
      </QueryPageState>
      <DetailDrawer
        open={Boolean(selectedId)}
        title={selected ? label(selected.id, selected.name) : t('inspector.notFound')}
        subtitle={selected ? t('machines.selectHint') : t('inspector.notFoundHint')}
        onClose={() => setSelectedId(null)}
      >
        {selected ? <MachineInspector machine={selected} farmId={farmId} /> : null}
      </DetailDrawer>
    </section>
  )
}
