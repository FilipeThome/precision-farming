import type { Operation } from '@/shared/api/types'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { Card } from '@/shared/ui/Card'

type Props = { operation: Operation }

/** Only rendered when a linked operation has completed: planned vs actual window. */
export function ExpectedVsObserved({ operation }: Props) {
  const { t } = useI18n()
  const { dateTime, label } = useFormat()
  return (
    <Card className="flex flex-col gap-2">
      <div className="flex items-center justify-between">
        <h2 className="font-display text-base font-bold">{t('decisions.compare.title')}</h2>
        <span className="rounded-full bg-ag-n-100 px-2 py-0.5 text-[11px] font-semibold text-ag-n-700">
          {label(operation.type)}
        </span>
      </div>
      <div className="grid grid-cols-3 gap-x-2 gap-y-1 text-xs">
        <span className="text-[11px] font-semibold uppercase tracking-[0.04em] text-ag-n-500">{t('decisions.compare.metric')}</span>
        <span className="text-[11px] font-semibold uppercase tracking-[0.04em] text-ag-n-500">{t('decisions.compare.expected')}</span>
        <span className="text-[11px] font-semibold uppercase tracking-[0.04em] text-ag-n-500">{t('decisions.compare.observed')}</span>
        <span>{t('decisions.compare.start')}</span>
        <span className="font-mono">{dateTime(operation.plannedStart)}</span>
        <span className="font-mono">{dateTime(operation.actualStart)}</span>
        <span>{t('decisions.compare.end')}</span>
        <span className="font-mono">{dateTime(operation.plannedEnd)}</span>
        <span className="font-mono">{dateTime(operation.actualEnd)}</span>
      </div>
      <p className="text-[11px] text-ag-n-500">{t('decisions.linkedOp.inferred')}</p>
    </Card>
  )
}
