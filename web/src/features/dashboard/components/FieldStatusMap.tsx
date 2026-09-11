import { ListChecks } from 'lucide-react'

import type { Field } from '@/shared/api/types'
import type { MessageKey } from '@/shared/i18n/useI18n'
import { useI18n } from '@/shared/i18n/useI18n'
import { FieldMap } from '@/shared/maps/FieldMap'
import { FIELD_STATE_COLORS, FIELD_STATE_LEGEND, type FieldState } from '@/shared/maps/fieldStateColors'

type Props = {
  fields: Field[]
  fieldStates: Record<string, FieldState>
  isLoading: boolean
  isError: boolean
  onFieldClick?: (fieldId: string) => void
}

const LEGEND_KEY: Record<FieldState, MessageKey> = {
  done: 'tower.map.legend.done',
  progress: 'tower.map.legend.progress',
  planned: 'tower.map.legend.planned',
  blocked: 'tower.map.legend.paused',
  stale: 'tower.map.legend.stale',
  none: 'tower.map.legend.none',
}

/** Leaflet field map colored by operation state. Only the "Status" layer exists — no fake layers. */
export function FieldStatusMap({ fields, fieldStates, isLoading, isError, onFieldClick }: Props) {
  const { t } = useI18n()

  if (isLoading) {
    return <div className="min-h-[320px] flex-1 animate-pulse rounded-[14px] bg-ag-n-100" aria-busy="true" />
  }
  if (isError) {
    return (
      <div className="grid min-h-[320px] flex-1 place-items-center rounded-[14px] border border-ag-n-200 bg-ag-n-0 text-sm text-ag-crit" role="alert">
        {t('map.loadError')}
      </div>
    )
  }
  if (fields.length === 0) {
    return (
      <div className="grid min-h-[320px] flex-1 place-items-center rounded-[14px] border border-ag-n-200 bg-ag-n-0 text-sm text-ag-n-600" role="status">
        {t('fields.emptyTitle')}
      </div>
    )
  }

  return (
    <div className="relative min-h-[320px] flex-1">
      <FieldMap
        fields={fields}
        fieldStates={fieldStates}
        onFieldClick={onFieldClick}
        className="h-full min-h-[320px]"
        minHeightClass="min-h-[320px]"
      />
      <div className="pointer-events-none absolute left-2.5 top-2.5 z-[500] flex gap-1.5">
        <span className="inline-flex items-center gap-1.5 rounded-full border border-ag-g-900 bg-ag-g-900 px-2.5 py-1 text-xs font-semibold text-white shadow-[0_1px_2px_rgba(20,30,25,0.06)]">
          <ListChecks className="h-3.5 w-3.5" aria-hidden />
          {t('tower.map.layer.status')}
        </span>
      </div>
      <dl className="absolute bottom-2.5 left-2.5 z-[500] grid grid-cols-2 gap-x-3 gap-y-1 rounded-[10px] border border-ag-n-200 bg-white/95 px-2.5 py-2 text-[11px] shadow-[0_1px_2px_rgba(20,30,25,0.06)]">
        {FIELD_STATE_LEGEND.map((state) => (
          <div key={state} className="flex items-center gap-1.5">
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
