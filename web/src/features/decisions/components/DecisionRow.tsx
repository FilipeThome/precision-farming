import { Link } from 'react-router'

import type { DecisionItem, DecisionSource } from '@/features/decisions/model'
import { useI18n } from '@/shared/i18n/useI18n'
import type { MessageKey } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { ConfidenceMeter } from '@/shared/ui/ConfidenceMeter'
import { EntityTile, type EntityKind } from '@/shared/ui/EntityTile'
import { FreshnessChip } from '@/shared/ui/FreshnessChip'
import { StatusBadge } from '@/shared/ui/StatusBadge'

export const SOURCE_KEY: Record<DecisionSource, MessageKey> = {
  PRESCRIPTION: 'decisions.source.PRESCRIPTION',
  IRRIGATION: 'decisions.source.IRRIGATION',
  AI_INSIGHT: 'decisions.source.AI_INSIGHT',
  AGRONOMY: 'decisions.source.AGRONOMY',
}

export const SOURCE_KIND: Record<DecisionSource, EntityKind> = {
  PRESCRIPTION: 'inventory',
  IRRIGATION: 'irrigation',
  AI_INSIGHT: 'recommendation',
  AGRONOMY: 'season',
}

/** List or detail URL that keeps farm + status so the filter cannot be wiped. */
export function decisionsHref(opts: {
  id?: string | null
  farmId?: string | null
  status?: string | null
}): string {
  const params = new URLSearchParams()
  if (opts.farmId) params.set('farm', opts.farmId)
  if (opts.status && opts.status !== 'pending') params.set('status', opts.status)
  const query = params.toString()
  const path = opts.id ? `/decisions/${encodeURIComponent(opts.id)}` : '/decisions'
  return query ? `${path}?${query}` : path
}

export function decisionHref(item: DecisionItem, status?: string | null): string {
  return decisionsHref({ id: item.id, farmId: item.farmId, status })
}

export function useDecisionTitle() {
  const { label, number } = useFormat()
  return (item: DecisionItem): string =>
    item.quantity ? `${label(item.title)} · ${number(item.quantity.value, 1)} ${item.quantity.unit}` : label(item.title)
}

type Props = { item: DecisionItem; selected: boolean; status?: string | null }

export function DecisionRow({ item, selected, status }: Props) {
  const { t } = useI18n()
  const { label } = useFormat()
  const title = useDecisionTitle()
  return (
    <li>
      <Link
        to={decisionHref(item, status)}
        aria-current={selected ? 'true' : undefined}
        className={`flex items-center gap-2.5 rounded-[10px] border px-2.5 py-2 transition hover:bg-ag-n-50 ${
          selected ? 'border-ag-t-500 bg-ag-t-50' : 'border-transparent'
        }`}
      >
        <EntityTile kind={SOURCE_KIND[item.source]} size="sm" tone={item.status === 'PENDING' ? 'teal' : 'brand'} />
        <span className="flex min-w-0 flex-1 flex-col gap-0.5">
          <span className="flex items-center justify-between gap-2">
            <b className="truncate text-[13px]">{title(item)}</b>
            <StatusBadge value={item.rawStatus ?? item.status} />
          </span>
          <span className="flex flex-wrap items-center gap-1.5 text-[11.5px] text-ag-n-600">
            <span>{t(SOURCE_KEY[item.source])}</span>
            {item.fieldId ? <span>· {label(item.fieldId)}</span> : null}
            <FreshnessChip at={item.createdAt} />
            <ConfidenceMeter value={item.confidence} showLabel={false} className="ml-auto" />
          </span>
        </span>
      </Link>
    </li>
  )
}
