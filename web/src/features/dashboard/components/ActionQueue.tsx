import type { ReactNode } from 'react'
import { BadgeCheck, Droplets, Eye, ShieldAlert } from 'lucide-react'
import { Link } from 'react-router'

import { useAckAlertMutation } from '@/features/alerts/queries'
import { needsHumanReview, type DecisionItem } from '@/features/decisions/model'
import { actionableDecisions, sortByPriority } from '@/features/decisions/selectors'
import type { Alert } from '@/shared/api/types'
import { useI18n } from '@/shared/i18n/useI18n'
import { queryError } from '@/shared/lib/queryError'
import { inspectHref } from '@/shared/lib/useFarmFromSearch'
import { useFormat } from '@/shared/lib/useFormat'
import { Button } from '@/shared/ui/Button'
import { ConfidenceMeter } from '@/shared/ui/ConfidenceMeter'
import { FreshnessChip } from '@/shared/ui/FreshnessChip'
import { TAG_TONE, type TagTone } from '@/shared/ui/StatusBadge'

import { TowerCard } from './TowerCard'

type Props = {
  decisions: DecisionItem[]
  alerts: Alert[]
  isLoading: boolean
  isError: boolean
  errorMessage?: string
  onRetry: () => void
}

type QueueEntry =
  | { kind: 'decision'; key: string; createdAt?: string; item: DecisionItem }
  | { kind: 'alert'; key: string; createdAt?: string; alert: Alert }

function Tag({ tone, children }: { tone: TagTone; children: ReactNode }) {
  return (
    <span className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[11px] font-semibold leading-4 ${TAG_TONE[tone]}`}>
      {children}
    </span>
  )
}

const LINK_CLASS =
  'inline-flex items-center justify-center rounded-[8px] border border-ag-n-300 bg-ag-n-0 px-2.5 py-1.5 text-xs font-semibold text-ag-g-800 hover:border-ag-t-500'

export function ActionQueue({ decisions, alerts, isLoading, isError, errorMessage, onRetry }: Props) {
  const { t } = useI18n()
  const { label, number } = useFormat()
  const ack = useAckAlertMutation()
  const ackErr = ack.error ? queryError(ack.error) : null

  const decisionEntries: QueueEntry[] = sortByPriority(actionableDecisions(decisions)).map((item) => ({
    kind: 'decision',
    key: item.id,
    createdAt: item.createdAt,
    item,
  }))
  const alertEntries: QueueEntry[] = alerts
    .filter((a) => a.status === 'OPEN' && a.severity === 'CRITICAL')
    .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
    .map((alert) => ({ kind: 'alert', key: `alert:${alert.id}`, createdAt: alert.createdAt, alert }))
  // Critical alerts first (severity), then decisions already sorted by priority.
  const entries = [...alertEntries, ...decisionEntries]

  return (
    <TowerCard
      title={t('tower.queue.title')}
      aside={<Tag tone="neutral">{t('tower.queue.count', { n: entries.length })}</Tag>}
      isLoading={isLoading}
      isError={isError}
      errorMessage={errorMessage}
      onRetry={onRetry}
      isEmpty={entries.length === 0}
      emptyText={t('tower.queue.empty')}
    >
      {ackErr ? (
        <p className="mb-2 text-xs text-ag-crit" role="alert">
          {ackErr.message}
        </p>
      ) : null}
      <ul className="flex flex-col gap-2">
        {entries.map((entry) => {
          if (entry.kind === 'alert') {
            const { alert } = entry
            return (
              <li key={entry.key} className="flex flex-col gap-2 rounded-[12px] border border-ag-n-200 bg-ag-n-0 px-3 py-2.5">
                <div className="flex items-center gap-2">
                  <Tag tone="crit">
                    <ShieldAlert className="h-3 w-3" aria-hidden />
                    {t('tower.queue.tag.critical')}
                  </Tag>
                  <Link to={inspectHref('/alerts', alert.id, alert.farmId)} className="min-w-0 flex-1 truncate text-[13px] font-semibold hover:underline">
                    {label(alert.title)}
                  </Link>
                </div>
                <div className="flex flex-wrap items-center gap-2 text-[11.5px] text-ag-n-600">
                  <FreshnessChip at={alert.createdAt} />
                  <span className="truncate">{label(alert.message)}</span>
                </div>
                <div className="flex items-center justify-end gap-1.5">
                  <Button size="sm" disabled={ack.isPending} onClick={() => ack.mutate(alert.id)}>
                    {t('alerts.ack')}
                  </Button>
                </div>
              </li>
            )
          }

          const { item } = entry
          const review = needsHumanReview(item)
          const to = `/decisions/${encodeURIComponent(item.id)}${item.farmId ? `?farm=${encodeURIComponent(item.farmId)}` : ''}`
          const title = item.quantity
            ? `${label(item.title)} · ${number(item.quantity.value, 1)} ${item.quantity.unit}`
            : label(item.title)
          return (
            <li key={entry.key} className="flex flex-col gap-2 rounded-[12px] border border-ag-n-200 bg-ag-n-0 px-3 py-2.5">
              <div className="flex items-center gap-2">
                {review ? (
                  <Tag tone="warn">
                    <Eye className="h-3 w-3" aria-hidden />
                    {t('tower.queue.tag.review')}
                  </Tag>
                ) : item.source === 'IRRIGATION' ? (
                  <Tag tone="teal">
                    <Droplets className="h-3 w-3" aria-hidden />
                    {t('tower.queue.tag.irrigation')}
                  </Tag>
                ) : (
                  <Tag tone="info">
                    <BadgeCheck className="h-3 w-3" aria-hidden />
                    {t('tower.queue.tag.approval')}
                  </Tag>
                )}
                <Link to={to} className="min-w-0 flex-1 truncate text-[13px] font-semibold hover:underline">
                  {title}
                </Link>
              </div>
              <div className="flex flex-wrap items-center gap-2 text-[11.5px] text-ag-n-600">
                <FreshnessChip at={item.createdAt} />
                {item.fieldId ? <span>{label(item.fieldId)}</span> : null}
                {item.priority ? <span>· {label(item.priority)}</span> : null}
                {item.model ? (
                  <span className="font-mono">
                    {item.model}
                    {item.modelVersion ? ` v${item.modelVersion}` : ''}
                  </span>
                ) : null}
              </div>
              <div className="flex items-center gap-2">
                <ConfidenceMeter value={item.confidence} hint={review ? t('tower.queue.reviewHint') : undefined} />
                <span className="ml-auto flex items-center gap-1.5">
                  {item.source === 'IRRIGATION' ? (
                    <Link to={item.farmId ? `/irrigation?farm=${encodeURIComponent(item.farmId)}` : '/irrigation'} className={LINK_CLASS}>
                      {t('tower.queue.openIrrigation')}
                    </Link>
                  ) : null}
                  <Link to={to} className={LINK_CLASS}>
                    {t('tower.queue.review')}
                  </Link>
                </span>
              </div>
            </li>
          )
        })}
      </ul>
    </TowerCard>
  )
}
