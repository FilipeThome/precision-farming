import type { DecisionItem } from '@/features/decisions/model'
import type { Operation } from '@/shared/api/types'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { Card } from '@/shared/ui/Card'

type Props = { item: DecisionItem; linked?: Operation }

type Event = { at: string; text: string; tone?: 'ok' | 'crit' | 'neutral' }

export function DecisionTimeline({ item, linked }: Props) {
  const { t } = useI18n()
  const { dateTime } = useFormat()

  const events: Event[] = []
  if (item.createdAt) {
    events.push({
      at: item.createdAt,
      text: item.confidence != null ? t('decisions.timeline.createdWithConfidence', { value: item.confidence.toFixed(2) }) : t('decisions.timeline.created'),
    })
  }
  if (item.approvedAt) events.push({ at: item.approvedAt, text: t('decisions.timeline.approved'), tone: 'ok' })
  if (linked?.actualStart) events.push({ at: linked.actualStart, text: t('decisions.timeline.executionStarted'), tone: 'neutral' })
  if (linked?.actualEnd) events.push({ at: linked.actualEnd, text: t('decisions.timeline.executionEnded'), tone: 'ok' })
  events.sort((a, b) => a.at.localeCompare(b.at))

  return (
    <Card className="flex flex-col gap-1">
      <h2 className="font-display text-base font-bold">{t('decisions.timeline.title')}</h2>
      {events.length === 0 ? (
        <p className="text-sm text-ag-n-600" role="status">
          {t('decisions.timeline.empty')}
        </p>
      ) : (
        <ol className="flex flex-col">
          {events.map((event, i) => (
            <li key={`${event.at}-${i}`} className="grid grid-cols-[auto_1fr] gap-x-2.5 border-b border-ag-n-100 py-1.5 text-xs last:border-b-0">
              <span className="font-mono text-[11px] text-ag-n-500">{dateTime(event.at)}</span>
              <span className={event.tone === 'ok' ? 'text-ag-ok' : event.tone === 'crit' ? 'text-ag-crit' : ''}>{event.text}</span>
            </li>
          ))}
        </ol>
      )}
      {linked ? <p className="mt-1 text-[11px] text-ag-n-500">{t('decisions.linkedOp.inferred')}</p> : null}
    </Card>
  )
}
