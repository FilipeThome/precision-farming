import { useInventoryQuery } from '@/features/inventory/queries'
import { useMachinesQuery } from '@/features/machines/queries'
import { OperationActions } from '@/features/operations/components/OpsBoard'
import { useOperationCommands } from '@/features/operations/queries'
import type { Operation } from '@/shared/api/types'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { EntityTile } from '@/shared/ui/EntityTile'
import { InspectorKpis } from '@/shared/ui/InspectorKpis'
import { InspectorQueryState } from '@/shared/ui/InspectorQueryState'
import { StatusBadge } from '@/shared/ui/StatusBadge'

type Commands = ReturnType<typeof useOperationCommands>

type Props = {
  operation: Operation
  farmId: string | null
  pauseReason: string
  commands: Commands
}

export function OperationInspector({ operation, farmId, pauseReason, commands }: Props) {
  const machines = useMachinesQuery(farmId)
  const inventory = useInventoryQuery(farmId)
  const { t } = useI18n()
  const { label, dateTime, number } = useFormat()
  const machine = (machines.data ?? []).find((item) => item.id === operation.machineId)
  const item = (inventory.data ?? []).find((row) => row.id === operation.itemId)
  const inputLabel =
    item && operation.itemQuantity != null
      ? `${label(item.name)} · ${number(Number(operation.itemQuantity), 1)} ${item.unit}`
      : '—'

  return (
    <div className="flex flex-col gap-4">
      <div className="flex gap-3">
        <EntityTile kind="operation" size="lg" label={label(operation.type)} />
        <StatusBadge value={operation.status} />
      </div>
      <InspectorQueryState
        isLoading={machines.isLoading || inventory.isLoading}
        error={machines.error || inventory.error}
        onRetry={() => {
          void machines.refetch()
          void inventory.refetch()
        }}
      >
        <InspectorKpis
          items={[
            { label: t('operations.kpi.planned'), value: dateTime(operation.plannedStart) },
            { label: t('operations.kpi.actual'), value: dateTime(operation.actualStart) },
            {
              label: t('operations.kpi.machine'),
              value: machine ? label(machine.id, machine.name) : '—',
            },
            { label: t('operations.kpi.inputs'), value: inputLabel },
          ]}
        />
      </InspectorQueryState>
      {operation.pauseReason ? (
        <p className="text-sm text-pf-muted">
          {t('operations.pauseMeta', { reason: label(operation.pauseReason) })}
        </p>
      ) : null}
      <OperationActions op={operation} pauseReason={pauseReason} commands={commands} />
    </div>
  )
}
