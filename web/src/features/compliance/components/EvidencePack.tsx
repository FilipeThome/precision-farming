import type { EvidencePack as EvidencePackDto } from '@/features/compliance/types'
import { useEvidencePackQuery } from '@/features/compliance/queries'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { queryError } from '@/shared/lib/queryError'
import { Button } from '@/shared/ui/Button'
import { Card } from '@/shared/ui/Card'
import { QueryPageState } from '@/shared/ui/QueryPageState'
import { SimulationNotice } from '@/shared/ui/SimulationNotice'
import { StatusBadge } from '@/shared/ui/StatusBadge'

type Props = { lotCode: string }

export function evidenceDownloadName(lotCode: string): string {
  const safe = lotCode.replace(/[^A-Za-z0-9_-]/g, '')
  return safe.length > 0 ? `evidence-${safe}.json` : 'evidence.json'
}

function downloadJson(pack: EvidencePackDto) {
  const blob = new Blob([JSON.stringify(pack, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = evidenceDownloadName(pack.lotCode)
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

export function EvidencePack({ lotCode }: Props) {
  const { t } = useI18n()
  const { label, date, number } = useFormat()
  const query = useEvidencePackQuery(lotCode)
  const err = queryError(query.error)
  const pack = query.data

  return (
    <div className="mt-4 print-evidence">
      <QueryPageState
        isLoading={query.isLoading}
        isError={query.isError}
        errorMessage={err.message}
        correlationId={err.correlationId}
        isEmpty={!query.isLoading && !query.data}
        emptyTitle={t('compliance.evidence.emptyTitle')}
        emptyDescription={t('compliance.evidence.emptyDescription')}
        onRetry={() => void query.refetch()}
      >
        {pack ? (
          <Card className="flex flex-col gap-3">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <h2 className="font-display text-lg font-bold">{t('compliance.evidence.title')}</h2>
              <div className="flex flex-wrap gap-2 print-hide">
                <Button variant="secondary" onClick={() => downloadJson(pack)}>
                  {t('compliance.evidence.download')}
                </Button>
                <Button variant="secondary" onClick={() => window.print()}>
                  {t('compliance.evidence.print')}
                </Button>
              </div>
            </div>
            <SimulationNotice simulation={pack.simulation === true} farm={pack.farmName || pack.farmId} />
            <dl className="grid gap-2 text-sm sm:grid-cols-2">
              <div>
                <dt className="text-[11px] font-semibold uppercase tracking-[0.04em] text-ag-n-500">
                  {t('compliance.lot.title', { lotCode: pack.lotCode })}
                </dt>
                <dd className="font-mono">{pack.lotCode}</dd>
              </div>
              <div>
                <dt className="text-[11px] font-semibold uppercase tracking-[0.04em] text-ag-n-500">
                  {t('decisions.prescription.receituario')}
                </dt>
                <dd>{label(pack.receituarioNumber)}</dd>
              </div>
              <div>
                <dt className="text-[11px] font-semibold uppercase tracking-[0.04em] text-ag-n-500">
                  {t('decisions.prescription.activeIngredient')}
                </dt>
                <dd>{label(pack.activeIngredient)}</dd>
              </div>
              <div>
                <dt className="text-[11px] font-semibold uppercase tracking-[0.04em] text-ag-n-500">
                  {t('decisions.prescription.moaGroup')}
                </dt>
                <dd>{label(pack.moaGroup)}</dd>
              </div>
              <div>
                <dt className="text-[11px] font-semibold uppercase tracking-[0.04em] text-ag-n-500">
                  {t('decisions.prescription.rtCpf')}
                </dt>
                <dd className="font-mono">{label(pack.responsibleTechCpf)}</dd>
              </div>
              <div>
                <dt className="text-[11px] font-semibold uppercase tracking-[0.04em] text-ag-n-500">
                  {t('decisions.prescription.phi')}
                </dt>
                <dd>{pack.phiDays != null ? number(Number(pack.phiDays), 0) : '—'}</dd>
              </div>
              <div>
                <dt className="text-[11px] font-semibold uppercase tracking-[0.04em] text-ag-n-500">
                  {t('compliance.credit.car')}
                </dt>
                <dd>{label(pack.carStatus)}</dd>
              </div>
              <div>
                <dt className="text-[11px] font-semibold uppercase tracking-[0.04em] text-ag-n-500">
                  {t('compliance.credit.embargo')}
                </dt>
                <dd>
                  <StatusBadge value={pack.embargoed ? 'EMBARGOED' : 'CLEAR'} />
                </dd>
              </div>
              <div>
                <dt className="text-[11px] font-semibold uppercase tracking-[0.04em] text-ag-n-500">
                  {t('compliance.evidence.cutoff')}
                </dt>
                <dd>{pack.deforestationCutoffDate ? date(pack.deforestationCutoffDate) : '—'}</dd>
              </div>
              <div className="sm:col-span-2">
                <dt className="text-[11px] font-semibold uppercase tracking-[0.04em] text-ag-n-500">
                  {t('compliance.evidence.geojson')}
                </dt>
                <dd className="mt-1 max-h-40 overflow-auto rounded-[8px] bg-ag-n-50 p-2 font-mono text-[11px]">
                  {JSON.stringify(pack.polygonGeoJson)}
                </dd>
              </div>
            </dl>
          </Card>
        ) : null}
      </QueryPageState>
    </div>
  )
}
