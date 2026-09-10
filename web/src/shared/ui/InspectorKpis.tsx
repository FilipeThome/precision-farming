import type { MessageKey } from '@/shared/i18n/useI18n'
import { useI18n } from '@/shared/i18n/useI18n'

export type InspectorKpi = { label: string; value: string; hint?: string }
export type TelemetrySource = 'LIVE' | 'STALE'

type InspectorKpisProps = {
  items: InspectorKpi[]
}

export function InspectorKpis({ items }: InspectorKpisProps) {
  return (
    <dl className="grid grid-cols-2 gap-2">
      {items.map((item) => (
        <div key={item.label} className="rounded-[12px] border border-pf-border bg-pf-bg px-3 py-2">
          <dt className="text-xs text-pf-muted">{item.label}</dt>
          <dd className="text-sm font-semibold text-pf-green">{item.value}</dd>
          {item.hint ? <p className="text-[11px] text-pf-muted">{item.hint}</p> : null}
        </div>
      ))}
    </dl>
  )
}

const SOURCE_KEY: Record<TelemetrySource, MessageKey> = {
  LIVE: 'inspector.source.live',
  STALE: 'inspector.source.stale',
}

const SOURCE_TONE: Record<TelemetrySource, string> = {
  LIVE: 'bg-teal-50 text-pf-teal',
  STALE: 'bg-amber-50 text-amber-900',
}

export function FreshnessChip({ source }: { source: TelemetrySource }) {
  const { t } = useI18n()
  return (
    <span className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${SOURCE_TONE[source]}`}>
      {t(SOURCE_KEY[source])}
    </span>
  )
}
