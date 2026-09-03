import type { Operation } from '@/shared/api/types'
import { farmPhoto, machinePhoto } from '@/shared/demo/media'
import type { MessageKey } from '@/shared/i18n/useI18n'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { Button } from '@/shared/ui/Button'
import { Card } from '@/shared/ui/Card'
import { EntityPhoto } from '@/shared/ui/EntityPhoto'
import { StatusBadge } from '@/shared/ui/StatusBadge'

import { OPS_STATUSES, type OpsStatus } from './OpsStatusFilters'

const COLUMN_KEYS: Record<OpsStatus, MessageKey> = {
  PLANNED: 'operations.column.PLANNED',
  IN_PROGRESS: 'operations.column.IN_PROGRESS',
  PAUSED: 'operations.column.PAUSED',
  COMPLETED: 'operations.column.COMPLETED',
}

type Commands = {
  start: { mutate: (id: string) => void; isPending: boolean }
  pause: { mutate: (args: { id: string; reason: string }) => void; isPending: boolean }
  complete: { mutate: (id: string) => void; isPending: boolean }
}

type Props = {
  operations: Operation[]
  pauseReason: string
  commands: Commands
}

function OperationActions({
  op,
  pauseReason,
  commands,
}: {
  op: Operation
  pauseReason: string
  commands: Commands
}) {
  const { t } = useI18n()
  const busy = commands.start.isPending || commands.pause.isPending || commands.complete.isPending

  return (
    <div className="flex flex-wrap gap-2">
      {(op.status === 'PLANNED' || op.status === 'PAUSED') && (
        <Button disabled={busy} onClick={() => commands.start.mutate(op.id)}>
          {t('operations.start')}
        </Button>
      )}
      {op.status === 'IN_PROGRESS' && (
        <Button
          variant="secondary"
          disabled={busy || !pauseReason.trim()}
          onClick={() => commands.pause.mutate({ id: op.id, reason: pauseReason })}
        >
          {t('operations.pause')}
        </Button>
      )}
      {(op.status === 'IN_PROGRESS' || op.status === 'PAUSED') && (
        <Button variant="secondary" disabled={busy} onClick={() => commands.complete.mutate(op.id)}>
          {t('operations.complete')}
        </Button>
      )}
    </div>
  )
}

export function OpsBoard({ operations, pauseReason, commands }: Props) {
  const { t } = useI18n()
  const { label, dateTime } = useFormat()

  return (
    <div className="grid gap-3 xl:grid-cols-4">
      {OPS_STATUSES.map((status) => {
        const column = operations.filter((op) => op.status === status)
        return (
          <div key={status} className="min-w-0">
            <h3 className="mb-2 text-sm font-semibold text-pf-green">
              {t(COLUMN_KEYS[status])} ({column.length})
            </h3>
            <div className="flex flex-col gap-2">
              {column.length === 0 ? (
                <Card className="border-dashed text-sm text-pf-muted">{t('operations.board.empty')}</Card>
              ) : (
                column.map((op) => (
                  <Card key={op.id} className="flex gap-3 p-3">
                    <EntityPhoto
                      src={machinePhoto(op.machineId) ?? farmPhoto(op.farmId)}
                      alt={label(op.type)}
                    />
                    <div className="flex min-w-0 flex-1 flex-col gap-2">
                      <div className="flex items-center gap-2">
                        <h2 className="font-semibold text-pf-green">{label(op.type)}</h2>
                        <StatusBadge value={op.status} />
                      </div>
                      <p className="text-xs text-pf-muted">
                        {t('operations.plannedStart', { when: dateTime(op.plannedStart) })}
                        {op.pauseReason
                          ? ` · ${t('operations.pauseMeta', { reason: label(op.pauseReason) })}`
                          : ''}
                      </p>
                      <OperationActions op={op} pauseReason={pauseReason} commands={commands} />
                    </div>
                  </Card>
                ))
              )}
            </div>
          </div>
        )
      })}
    </div>
  )
}

export function OpsList({ operations, pauseReason, commands }: Props) {
  const { t } = useI18n()
  const { label, dateTime } = useFormat()

  return (
    <div className="flex flex-col gap-3">
      {operations.map((op) => (
        <Card key={op.id} className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex min-w-0 items-center gap-3">
            <EntityPhoto
              variant="thumb"
              src={machinePhoto(op.machineId) ?? farmPhoto(op.farmId)}
              alt={label(op.type)}
            />
            <div>
              <div className="flex items-center gap-2">
                <h2 className="font-semibold text-pf-green">{label(op.type)}</h2>
                <StatusBadge value={op.status} />
              </div>
              <p className="mt-1 text-xs text-pf-muted">
                {t('operations.plannedStart', { when: dateTime(op.plannedStart) })}
                {op.pauseReason ? ` · ${t('operations.pauseMeta', { reason: label(op.pauseReason) })}` : ''}
              </p>
            </div>
          </div>
          <OperationActions op={op} pauseReason={pauseReason} commands={commands} />
        </Card>
      ))}
    </div>
  )
}
