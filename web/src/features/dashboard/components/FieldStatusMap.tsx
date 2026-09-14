import { AlertTriangle, ListChecks } from 'lucide-react'

import type { Field } from '@/shared/api/types'
import type { MessageKey } from '@/shared/i18n/useI18n'
import { useI18n } from '@/shared/i18n/useI18n'
import { FieldMap } from '@/shared/maps/FieldMap'
import { FIELD_STATE_COLORS, FIELD_STATE_LEGEND, type FieldState } from '@/shared/maps/fieldStateColors'
import { Button } from '@/shared/ui/Button'

type Props = {
  fields: Field[]
  fieldStates: Record<string, FieldState>
  isLoading: boolean
  isError: boolean
  errorMessage?: string
  correlationId?: string
  onRetry?: () => void
  onFieldClick?: (fieldId: string) => void
  /** Tailwind min-height for the map canvas (Tower widget vs full /map page). */
  minHeightClass?: string
}

const LEGEND_KEY: Record<FieldState, MessageKey> = {
  done: 'tower.map.legend.done',
  progress: 'tower.map.legend.progress',
  planned: 'tower.map.legend.planned',
  blocked: 'tower.map.legend.paused',
  stale: 'tower.map.legend.stale',
  none: 'tower.map.legend.none',
}

const DEFAULT_MIN_HEIGHT = 'min-h-[420px]'

/** Leaflet field map colored by operation state. Only the "Status" layer exists — no fake layers. */
export function FieldStatusMap({
  fields,
  fieldStates,
  isLoading,
  isError,
  errorMessage,
  correlationId,
  onRetry,
  onFieldClick,
  minHeightClass = DEFAULT_MIN_HEIGHT,
}: Props) {
  const { t } = useI18n()
  const frame = `flex-1 min-h-0 ${minHeightClass}`

  if (isLoading) {
    return <div className={`${frame} animate-pulse rounded-[14px] bg-ag-n-100`} role="status" aria-busy="true" />
  }
  if (isError) {
    return (
      <div
        className={`flex ${frame} flex-col items-start justify-center gap-3 rounded-[14px] border border-ag-n-200 bg-ag-n-0 p-4`}
        role="alert"
      >
        <div className="flex items-center gap-2 text-ag-crit">
          <AlertTriangle className="h-5 w-5" aria-hidden />
          <strong>{t('common.loadError')}</strong>
        </div>
        <p className="text-sm text-ag-n-600">{errorMessage ?? t('map.loadError')}</p>
        {correlationId ? (
          <p className="text-xs text-ag-n-600">
            {t('common.correlationId')} <code>{correlationId}</code>
          </p>
        ) : null}
        {onRetry ? (
          <Button onClick={onRetry} variant="secondary">
            {t('common.retry')}
          </Button>
        ) : null}
      </div>
    )
  }
  if (fields.length === 0) {
    return (
      <div className={`grid ${frame} place-items-center rounded-[14px] border border-ag-n-200 bg-ag-n-0 text-sm text-ag-n-600`} role="status">
        {t('fields.emptyTitle')}
      </div>
    )
  }

  return (
    <div className={`relative ${frame}`}>
      <FieldMap
        fields={fields}
        fieldStates={fieldStates}
        onFieldClick={onFieldClick}
        className={`h-full ${minHeightClass}`}
        minHeightClass={minHeightClass}
      />
      <div className="pointer-events-none absolute left-[46px] top-2.5 z-[500] flex gap-1.5">
        <span className="inline-flex items-center gap-1.5 rounded-full border border-ag-n-200 bg-white px-2.5 py-1 text-xs font-semibold text-ag-n-800 shadow-[0_1px_2px_rgba(20,30,25,0.06)]">
          <ListChecks className="h-3.5 w-3.5 text-ag-g-700" aria-hidden />
          {t('tower.map.layer.status')}
        </span>
      </div>
      <dl className="absolute bottom-7 left-2.5 z-[500] grid min-w-[248px] grid-cols-2 gap-x-4 gap-y-1 rounded-[10px] border border-ag-n-200 bg-white/95 px-2.5 py-2 text-[11px] text-ag-n-800 shadow-[0_1px_2px_rgba(20,30,25,0.06)]">
        {FIELD_STATE_LEGEND.map((state) => (
          <div key={state} className="flex items-center gap-1.5 whitespace-nowrap">
            <dt>
              <span
                className="inline-block h-2.5 w-2.5 rounded-[3px]"
                style={{
                  background: FIELD_STATE_COLORS[state].fill,
                  outline: FIELD_STATE_COLORS[state].dash ? `2px dashed ${FIELD_STATE_COLORS[state].stroke}` : undefined,
                  outlineOffset: '-2px',
                }}
                aria-hidden
              />
              <span className="sr-only">{t(LEGEND_KEY[state])}</span>
            </dt>
            <dd>{t(LEGEND_KEY[state])}</dd>
          </div>
        ))}
      </dl>
    </div>
  )
}
