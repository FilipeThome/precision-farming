import { useState } from 'react'

import { OperationInspector } from '@/features/operations/components/OperationInspector'
import { OpsBoard, OpsList } from '@/features/operations/components/OpsBoard'
import {
  OpsStatusFilters,
  type OpsStatusFilter,
} from '@/features/operations/components/OpsStatusFilters'
import { useOperationCommands, useOperationsQuery } from '@/features/operations/queries'
import { ApiError } from '@/shared/api/client'
import { opsStatusBars } from '@/shared/charts/adapters'
import { useI18n } from '@/shared/i18n/useI18n'
import type { MessageKey } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { queryError } from '@/shared/lib/queryError'
import { useSelectedId } from '@/shared/lib/useSelectedId'
import { Button } from '@/shared/ui/Button'
import { CHART_COLORS, ChartCard } from '@/shared/ui/ChartCard'
import { BarChartBlock } from '@/shared/ui/charts'
import { DetailDrawer } from '@/shared/ui/DetailDrawer'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'
import { useUiStore } from '@/shared/ui/uiStore'

type ViewMode = 'list' | 'board'

function commandErrorCopy(
  error: unknown,
  t: (key: MessageKey) => string,
): { message: string; correlationId?: string } {
  if (error instanceof ApiError && error.code === 'PRESCRIPTION_NOT_APPROVED') {
    return {
      message: t('operations.error.PRESCRIPTION_NOT_APPROVED'),
      correlationId: error.correlationId,
    }
  }
  return queryError(error)
}

export function OperationsPage() {
  const farmId = useUiStore((s) => s.farmId)
  const operations = useOperationsQuery(farmId)
  const commands = useOperationCommands()
  const { t } = useI18n()
  const { label } = useFormat()
  const [viewMode, setViewMode] = useState<ViewMode>('list')
  const [statusFilter, setStatusFilter] = useState<OpsStatusFilter>('ALL')
  const [pauseReason, setPauseReason] = useState(() => t('operations.pauseReasonDefault'))
  const { selectedId, setSelectedId } = useSelectedId()
  const selected = (operations.data ?? []).find((op) => op.id === selectedId)
  const err = queryError(operations.error)
  const rawCommandError = commands.start.error || commands.pause.error || commands.complete.error
  const commandError = rawCommandError ? commandErrorCopy(rawCommandError, t) : null

  const filtered = (operations.data ?? []).filter(
    (op) => statusFilter === 'ALL' || op.status === statusFilter,
  )

  return (
    <section>
      <PageHeader title={t('operations.title')} description={t('operations.description')} />
      <div className="mb-4 flex flex-wrap gap-2">
        <Button
          variant={viewMode === 'list' ? 'primary' : 'secondary'}
          aria-pressed={viewMode === 'list'}
          onClick={() => setViewMode('list')}
        >
          {t('operations.view.list')}
        </Button>
        <Button
          variant={viewMode === 'board' ? 'primary' : 'secondary'}
          aria-pressed={viewMode === 'board'}
          onClick={() => setViewMode('board')}
        >
          {t('operations.view.board')}
        </Button>
      </div>
      <OpsStatusFilters value={statusFilter} onChange={setStatusFilter} />
      <label className="mb-4 flex max-w-md flex-col gap-1 text-sm">
        {t('operations.pauseReason')}
        <input
          value={pauseReason}
          onChange={(e) => setPauseReason(e.target.value)}
          className="rounded-[12px] border border-pf-border bg-white px-3 py-2"
        />
      </label>
      {commandError ? (
        <p className="mb-3 text-sm text-red-800" role="alert">
          {commandError.message}
          {commandError.correlationId
            ? ` · ${t('common.correlationId')} ${commandError.correlationId}`
            : ''}
        </p>
      ) : null}
      {(operations.data?.length ?? 0) > 0 ? (
        <ChartCard title={t('charts.opsByStatus')} className="mb-4">
          <BarChartBlock
            data={opsStatusBars(operations.data ?? [], label)}
            xKey="name"
            bars={[{ dataKey: 'value', name: t('charts.count'), color: CHART_COLORS.green }]}
          />
        </ChartCard>
      ) : null}
      <QueryPageState
        isLoading={operations.isLoading}
        isError={operations.isError}
        errorMessage={err.message}
        correlationId={err.correlationId}
        isEmpty={!operations.isLoading && filtered.length === 0}
        emptyTitle={t('operations.emptyTitle')}
        emptyDescription={t('operations.emptyDescription')}
        onRetry={() => void operations.refetch()}
      >
        {viewMode === 'board' ? (
          <OpsBoard
            operations={filtered}
            pauseReason={pauseReason}
            commands={commands}
            selectedId={selectedId}
            onSelect={setSelectedId}
          />
        ) : (
          <OpsList
            operations={filtered}
            pauseReason={pauseReason}
            commands={commands}
            selectedId={selectedId}
            onSelect={setSelectedId}
          />
        )}
      </QueryPageState>
      <DetailDrawer
        open={Boolean(selectedId)}
        title={selected ? label(selected.type) : t('inspector.notFound')}
        subtitle={selected ? undefined : t('inspector.notFoundHint')}
        onClose={() => setSelectedId(null)}
      >
        {selected ? (
          <OperationInspector
            operation={selected}
            farmId={farmId}
            pauseReason={pauseReason}
            commands={commands}
          />
        ) : null}
      </DetailDrawer>
    </section>
  )
}
