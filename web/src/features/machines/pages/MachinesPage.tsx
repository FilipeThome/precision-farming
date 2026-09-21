import { useState } from 'react'

import { MachineFormDialog } from '@/features/machines/components/MachineFormDialog'
import { MachineInspector } from '@/features/machines/components/MachineInspector'
import { useMachinesQuery } from '@/features/machines/queries'
import { useMachinePhoto } from '@/features/machines/useMachinePhoto'
import { useCanWriteFleet } from '@/shared/auth/roles'
import type { Machine } from '@/shared/api/types'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { queryError } from '@/shared/lib/queryError'
import { useSelectedId } from '@/shared/lib/useSelectedId'
import { Button } from '@/shared/ui/Button'
import { DetailDrawer } from '@/shared/ui/DetailDrawer'
import { EntityCard } from '@/shared/ui/EntityCard'
import { InspectorEditButton } from '@/shared/ui/InspectorEditButton'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'
import { StatusBadge } from '@/shared/ui/StatusBadge'
import { useUiStore } from '@/shared/ui/uiStore'

function MachineCard({
  machine,
  selected,
  onSelect,
}: {
  machine: Machine
  selected: boolean
  onSelect: () => void
}) {
  const photo = useMachinePhoto(machine.photoFileId)
  const { label } = useFormat()
  return (
    <EntityCard
      title={label(machine.id, machine.name)}
      subtitle={`${label(machine.type)} · ${machine.manufacturer} ${machine.model}`}
      kind="machine"
      machineType={machine.type}
      photoUrl={photo.data}
      selected={selected}
      onSelect={onSelect}
    >
      <StatusBadge value={machine.status} />
    </EntityCard>
  )
}

export function MachinesPage() {
  const farmId = useUiStore((s) => s.farmId)
  const machines = useMachinesQuery(farmId)
  const err = queryError(machines.error)
  const { t } = useI18n()
  const { label } = useFormat()
  const { selectedId, setSelectedId } = useSelectedId()
  const selected = (machines.data ?? []).find((machine) => machine.id === selectedId)
  const canWrite = useCanWriteFleet()
  const [form, setForm] = useState<'create' | 'edit' | null>(null)

  return (
    <section>
      <PageHeader
        title={t('machines.title')}
        description={t('machines.description')}
        actions={
          canWrite ? (
            <Button type="button" onClick={() => setForm('create')}>
              {t('form.new')}
            </Button>
          ) : null
        }
      />
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
            <MachineCard
              key={machine.id}
              machine={machine}
              selected={machine.id === selectedId}
              onSelect={() => setSelectedId(machine.id)}
            />
          ))}
        </div>
      </QueryPageState>
      <DetailDrawer
        open={Boolean(selectedId)}
        title={selected ? label(selected.id, selected.name) : t('inspector.notFound')}
        subtitle={selected ? t('machines.selectHint') : t('inspector.notFoundHint')}
        onClose={() => setSelectedId(null)}
      >
        {selected ? (
          <>
            <MachineInspector machine={selected} farmId={farmId} />
            {canWrite ? <InspectorEditButton onEdit={() => setForm('edit')} /> : null}
          </>
        ) : null}
      </DetailDrawer>
      <MachineFormDialog
        open={form !== null}
        machine={form === 'edit' ? selected ?? null : null}
        onClose={() => setForm(null)}
      />
    </section>
  )
}
