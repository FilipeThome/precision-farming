import { useCreditDossierQuery } from '@/features/compliance/queries'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { queryError } from '@/shared/lib/queryError'
import { Card } from '@/shared/ui/Card'
import { QueryPageState } from '@/shared/ui/QueryPageState'
import { SimulationNotice } from '@/shared/ui/SimulationNotice'
import { StatusBadge } from '@/shared/ui/StatusBadge'

type Props = { farmId?: string | null }

export function CreditDossierPanel({ farmId }: Props) {
  const { t } = useI18n()
  const { label, date } = useFormat()
  const query = useCreditDossierQuery(farmId)
  const err = queryError(query.error)
  const dossier = query.data

  if (!farmId) {
    return (
      <QueryPageState
        isLoading={false}
        isError={false}
        isEmpty
        emptyTitle={t('compliance.credit.emptyTitle')}
        emptyDescription={t('compliance.credit.emptyDescription')}
      >
        {null}
      </QueryPageState>
    )
  }

  return (
    <QueryPageState
      isLoading={query.isLoading}
      isError={query.isError}
      errorMessage={err.message}
      correlationId={err.correlationId}
      isEmpty={!query.isLoading && !dossier}
      emptyTitle={t('compliance.credit.emptyTitle')}
      emptyDescription={t('compliance.credit.emptyDescription')}
      onRetry={() => void query.refetch()}
    >
      {dossier ? (
        <Card className="flex flex-col gap-3">
          <SimulationNotice simulation={dossier.simulation === true} farm={dossier.farmId} />
          <dl className="grid gap-3 text-sm sm:grid-cols-2">
            <div>
              <dt className="text-[11px] font-semibold uppercase tracking-[0.04em] text-ag-n-500">
                {t('compliance.credit.car')}
              </dt>
              <dd className="mt-1 flex flex-wrap items-center gap-2">
                <span>{label(dossier.carCode)}</span>
                {dossier.carStatus ? <StatusBadge value={dossier.carStatus} /> : null}
              </dd>
            </div>
            <div>
              <dt className="text-[11px] font-semibold uppercase tracking-[0.04em] text-ag-n-500">
                {t('compliance.credit.embargo')}
              </dt>
              <dd className="mt-1">
                <StatusBadge value={dossier.embargoed ? 'EMBARGOED' : 'CLEAR'} />
              </dd>
            </div>
            <div>
              <dt className="text-[11px] font-semibold uppercase tracking-[0.04em] text-ag-n-500">
                {t('compliance.credit.deforestation')}
              </dt>
              <dd className="mt-1 flex flex-wrap items-center gap-2">
                <StatusBadge value={dossier.deforestationClear ? 'CLEAR' : 'FLAGGED'} />
                {dossier.deforestationCutoffDate ? (
                  <span className="text-xs text-ag-n-600">{date(dossier.deforestationCutoffDate)}</span>
                ) : null}
              </dd>
            </div>
            <div>
              <dt className="text-[11px] font-semibold uppercase tracking-[0.04em] text-ag-n-500">
                {t('compliance.credit.zarc')}
              </dt>
              <dd className="mt-1">
                <StatusBadge value={dossier.zarcCompliant ? 'COMPLIANT' : 'NON_COMPLIANT'} />
              </dd>
            </div>
            <div className="sm:col-span-2">
              <dt className="text-[11px] font-semibold uppercase tracking-[0.04em] text-ag-n-500">
                {t('compliance.credit.remoteSensing')}
              </dt>
              <dd className="mt-1">{dossier.remoteSensingNote ? label(dossier.remoteSensingNote) : '—'}</dd>
            </div>
          </dl>
        </Card>
      ) : null}
    </QueryPageState>
  )
}
