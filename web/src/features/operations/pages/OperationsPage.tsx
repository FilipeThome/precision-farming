import { useState } from 'react'

import { useOperationCommands, useOperationsQuery } from '@/features/operations/queries'
import { useI18n } from '@/shared/i18n/useI18n'
import { formatDateTime } from '@/shared/lib/format'
import { queryError } from '@/shared/lib/queryError'
import { Button } from '@/shared/ui/Button'
import { Card } from '@/shared/ui/Card'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'
import { StatusBadge } from '@/shared/ui/StatusBadge'
import { useUiStore } from '@/shared/ui/uiStore'

export function OperationsPage() {
  const farmId = useUiStore((s) => s.farmId)
  const operations = useOperationsQuery(farmId)
  const commands = useOperationCommands()
  const { t } = useI18n()
  const [pauseReason, setPauseReason] = useState('Pausa solicitada pelo operador')
  const err = queryError(operations.error)
  const commandError =
    commands.start.error || commands.pause.error || commands.complete.error
      ? queryError(commands.start.error || commands.pause.error || commands.complete.error)
      : null
  const busy = commands.start.isPending || commands.pause.isPending || commands.complete.isPending

  return (
    <section>
      <PageHeader title={t('operations.title')} description={t('operations.description')} />
      <label className="mb-4 flex max-w-md flex-col gap-1 text-sm">
        Motivo da pausa
        <input
          value={pauseReason}
          onChange={(e) => setPauseReason(e.target.value)}
          className="rounded-[12px] border border-pf-border bg-white px-3 py-2"
        />
      </label>
      {commandError ? (
        <p className="mb-3 text-sm text-red-800" role="alert">
          {commandError.message}
        </p>
      ) : null}
      <QueryPageState
        isLoading={operations.isLoading}
        isError={operations.isError}
        errorMessage={err.message}
        correlationId={err.correlationId}
        isEmpty={!operations.isLoading && (operations.data?.length ?? 0) === 0}
        emptyTitle={t('operations.emptyTitle')}
        emptyDescription={t('operations.emptyDescription')}
        onRetry={() => void operations.refetch()}
      >
        <div className="flex flex-col gap-3">
          {(operations.data ?? []).map((op) => (
            <Card key={op.id} className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <div className="flex items-center gap-2">
                  <h2 className="font-semibold text-pf-green">{op.type}</h2>
                  <StatusBadge value={op.status} />
                </div>
                <p className="mt-1 text-xs text-pf-muted">
                  Início planejado: {formatDateTime(op.plannedStart)}
                  {op.pauseReason ? ` · Pausa: ${op.pauseReason}` : ''}
                </p>
              </div>
              <div className="flex flex-wrap gap-2">
                {(op.status === 'PLANNED' || op.status === 'PAUSED') && (
                  <Button disabled={busy} onClick={() => commands.start.mutate(op.id)}>
                    Iniciar
                  </Button>
                )}
                {op.status === 'IN_PROGRESS' && (
                  <Button
                    variant="secondary"
                    disabled={busy || !pauseReason.trim()}
                    onClick={() => commands.pause.mutate({ id: op.id, reason: pauseReason })}
                  >
                    Pausar
                  </Button>
                )}
                {(op.status === 'IN_PROGRESS' || op.status === 'PAUSED') && (
                  <Button variant="secondary" disabled={busy} onClick={() => commands.complete.mutate(op.id)}>
                    Concluir
                  </Button>
                )}
              </div>
            </Card>
          ))}
        </div>
      </QueryPageState>
    </section>
  )
}
