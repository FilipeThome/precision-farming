import { Link } from 'react-router'

import type { DecisionItem } from '@/features/decisions/model'
import { chainSteps, linkedOperation, type ChainStepId } from '@/features/decisions/chain'
import { MoaRotationStrip } from '@/features/decisions/components/MoaRotationStrip'
import { PrescriptionFieldsCard } from '@/features/decisions/components/PrescriptionFieldsCard'
import { SpraySavingsCard } from '@/features/decisions/components/SpraySavingsCard'
import { WeatherWindowHint } from '@/features/decisions/components/WeatherWindowHint'
import type { Operation } from '@/shared/api/types'
import type { MessageKey } from '@/shared/i18n/useI18n'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { Card } from '@/shared/ui/Card'
import { ChainRail } from '@/shared/ui/ChainRail'
import { ConfidenceMeter } from '@/shared/ui/ConfidenceMeter'
import { FreshnessChip } from '@/shared/ui/FreshnessChip'
import { StatusBadge, TAG_TONE } from '@/shared/ui/StatusBadge'

import { ApprovalPanel } from './ApprovalPanel'
import { SOURCE_KEY, useDecisionTitle } from './DecisionRow'
import { DecisionTimeline } from './DecisionTimeline'
import { ExpectedVsObserved } from './ExpectedVsObserved'

const STEP_KEY: Record<ChainStepId, MessageKey> = {
  signal: 'chain.signal',
  context: 'chain.context',
  recommendation: 'chain.recommendation',
  approval: 'chain.approval',
  order: 'chain.order',
  execution: 'chain.execution',
}

type Props = { item: DecisionItem; operations: Operation[] }

export function DecisionDetail({ item, operations }: Props) {
  const { t } = useI18n()
  const { label } = useFormat()
  const title = useDecisionTitle()
  const linked = linkedOperation(item, operations)
  const isRx = item.source === 'PRESCRIPTION'
  const steps = chainSteps(item, operations).map((step) => ({
    id: step.id,
    label: t(STEP_KEY[step.id]),
    state: step.state,
    hint: step.inferred ? t('decisions.linkedOp.inferred') : undefined,
  }))
  const dataLines = item.explanation && item.explanation.length > 0 ? item.explanation : item.summary ? [item.summary] : []
  const opsHref = item.farmId
    ? `/operations?farm=${encodeURIComponent(item.farmId)}`
    : '/operations'

  return (
    <div className="flex flex-col gap-3.5">
      <div className="flex flex-col gap-2.5">
        <p className="flex items-center gap-1.5 text-xs text-ag-n-500">
          <span>{t('domain.decisions')}</span>
          <span aria-hidden>›</span>
          <span className="font-mono">{item.rawId}</span>
        </p>
        <div className="flex flex-wrap items-center gap-2">
          <h2 className="font-display text-[22px] font-bold text-ag-n-900">{title(item)}</h2>
          <StatusBadge value={item.rawStatus ?? item.status} />
          <span className={`rounded-full px-2 py-0.5 text-[11px] font-semibold ${TAG_TONE.neutral}`}>{t(SOURCE_KEY[item.source])}</span>
          {item.fieldId ? (
            <span className={`rounded-full px-2 py-0.5 text-[11px] font-semibold ${TAG_TONE.neutral}`}>{label(item.fieldId)}</span>
          ) : null}
        </div>
        <Card className="px-3.5 pb-2 pt-1 shadow-none">
          <ChainRail steps={steps} aria-label={t('chain.label')} />
        </Card>
      </div>

      <div className="grid gap-3.5 xl:grid-cols-[1fr_1.15fr_340px]">
        <div className="flex flex-col gap-3.5">
          <Card className="flex flex-col gap-2">
            <h3 className="font-display text-base font-bold">{t('decisions.context.title')}</h3>
            <p className="text-[13px]">{item.summary ? label(item.summary) : title(item)}</p>
            {dataLines.length > 0 ? (
              <ul className="flex flex-col gap-1 text-xs">
                {dataLines.map((line, i) => (
                  <li key={`${line}-${i}`} className="border-b border-ag-n-100 py-1 last:border-b-0">
                    <b>{label(line)}</b>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="text-sm text-ag-n-600" role="status">{t('decisions.data.empty')}</p>
            )}
            <FreshnessChip at={item.createdAt} />
          </Card>
          {isRx ? <PrescriptionFieldsCard prescriptionId={item.rawId} showApprove={false} /> : null}
          {isRx ? <MoaRotationStrip fieldId={item.fieldId} /> : null}
        </div>

        <div className="flex flex-col gap-3.5">
          {item.confidence != null ? (
            <Card className="flex flex-col gap-3">
              <h3 className="font-display text-base font-bold">{t('decisions.confidence.title')}</h3>
              <ConfidenceMeter value={item.confidence} size="lg" />
            </Card>
          ) : (
            <Card>
              <h3 className="font-display text-base font-bold">{t('decisions.confidence.title')}</h3>
              <p className="mt-1 text-sm text-ag-n-600" role="status">{t('decisions.confidence.none')}</p>
            </Card>
          )}
          {isRx ? <SpraySavingsCard prescriptionId={item.rawId} /> : null}
          {isRx ? <WeatherWindowHint farmId={item.farmId} /> : null}
          {isRx ? (
            <Link
              to={opsHref}
              className="inline-flex items-center justify-center rounded-[8px] bg-ag-t-600 px-3 py-2 text-[13px] font-semibold text-white hover:bg-ag-t-700"
            >
              {t('decisions.dispatch.openOps')}
            </Link>
          ) : null}
        </div>

        <div className="flex flex-col gap-3.5">
          <ApprovalPanel item={item} />
          <DecisionTimeline item={item} linked={linked} />
          {linked && linked.status === 'COMPLETED' ? <ExpectedVsObserved operation={linked} /> : null}
        </div>
      </div>
    </div>
  )
}
