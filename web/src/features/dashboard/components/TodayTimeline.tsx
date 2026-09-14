import { Link } from 'react-router'

import { HOUR_TICKS, type TimelineBand, type TimelineRow } from '@/features/dashboard/model/todayTimeline'
import { useI18n } from '@/shared/i18n/useI18n'
import { inspectHref } from '@/shared/lib/useFarmFromSearch'
import { useFormat } from '@/shared/lib/useFormat'
import { StatusBadge } from '@/shared/ui/StatusBadge'

import { TowerCard } from './TowerCard'

type Props = {
  rows: TimelineRow[]
  bands: TimelineBand[]
  isLoading: boolean
  isError: boolean
  errorMessage?: string
  onRetry: () => void
}

const BAND_CLASS: Record<string, string> = {
  FAVORABLE: 'bg-ag-ok-bg',
  MARGINAL: 'bg-ag-warn-bg',
  UNFAVORABLE: 'bg-ag-crit-bg',
}

function Legend() {
  const { t } = useI18n()
  return (
    <div className="flex flex-wrap items-center gap-3 text-xs text-ag-n-600">
      <span className="flex items-center gap-1.5">
        <i className="inline-block h-1.5 w-2.5 rounded-[3px] bg-ag-t-600" aria-hidden />
        {t('tower.timeline.executed')}
      </span>
      <span className="flex items-center gap-1.5">
        <i className="inline-block h-1.5 w-2.5 rounded-[3px] border border-dashed border-ag-g-600" aria-hidden />
        {t('tower.timeline.planned')}
      </span>
      <span className="flex items-center gap-1.5">
        <i className="inline-block h-1.5 w-2.5 bg-ag-ok-bg" aria-hidden />
        {t('tower.timeline.goodWindow')}
      </span>
      <span className="flex items-center gap-1.5">
        <i className="inline-block h-1.5 w-2.5 bg-ag-crit-bg" aria-hidden />
        {t('tower.timeline.badWindow')}
      </span>
    </div>
  )
}

/** "Ordens de hoje · planejado vs executado" — 24h track with weather-window bands per operation. */
export function TodayTimeline({ rows, bands, isLoading, isError, errorMessage, onRetry }: Props) {
  const { t } = useI18n()
  const { label } = useFormat()

  return (
    <TowerCard
      title={t('tower.timeline.title')}
      aside={<Legend />}
      isLoading={isLoading}
      isError={isError}
      errorMessage={errorMessage}
      onRetry={onRetry}
      isEmpty={rows.length === 0}
      emptyText={t('tower.timeline.empty')}
    >
      <div
        className="grid grid-cols-[minmax(120px,160px)_1fr] items-center gap-x-2.5 gap-y-1.5 text-[11px]"
        title={t('tower.timeline.localDayHint')}
      >
        <span />
        <div className="flex justify-between font-mono text-[10px] text-ag-n-500" aria-hidden>
          {HOUR_TICKS.map((h) => (
            <span key={h}>{String(h).padStart(2, '0')}h</span>
          ))}
        </div>
        {rows.map(({ operation, planned, executed }) => (
          <TimelineRowView
            key={operation.id}
            to={inspectHref('/operations', operation.id, operation.farmId)}
            name={`${label(operation.type)} · ${label(operation.fieldId)}`}
            status={operation.status}
            planned={planned}
            executed={executed}
            bands={bands}
          />
        ))}
      </div>
    </TowerCard>
  )
}

function TimelineRowView({
  to,
  name,
  status,
  planned,
  executed,
  bands,
}: {
  to: string
  name: string
  status: string
  planned?: TimelineRow['planned']
  executed?: TimelineRow['executed']
  bands: TimelineBand[]
}) {
  return (
    <>
      <Link to={to} className="flex min-w-0 items-center gap-1.5 truncate hover:underline">
        <span className="truncate font-semibold">{name}</span>
        {status === 'PAUSED' ? <StatusBadge value={status} className="px-1.5 py-0" /> : null}
      </Link>
      <div className="relative h-[18px] overflow-hidden rounded-[6px] bg-ag-n-100" role="img" aria-label={name}>
        {bands.map((band, i) => (
          <span
            key={i}
            className={`absolute inset-y-0 ${BAND_CLASS[band.rating] ?? 'bg-ag-n-100'}`}
            style={{ left: `${band.leftPct}%`, width: `${band.widthPct}%` }}
            aria-hidden
          />
        ))}
        {planned ? (
          <span
            className="absolute top-1 h-2.5 rounded-[4px] border-[1.5px] border-dashed border-ag-g-600 bg-[rgba(42,115,80,0.12)]"
            style={{ left: `${planned.leftPct}%`, width: `${planned.widthPct}%` }}
            data-role="planned"
          />
        ) : null}
        {executed ? (
          <span
            className="absolute top-1 h-2.5 rounded-[4px] bg-ag-t-600"
            style={{ left: `${executed.leftPct}%`, width: `${executed.widthPct}%` }}
            data-role="executed"
          />
        ) : null}
      </div>
    </>
  )
}
