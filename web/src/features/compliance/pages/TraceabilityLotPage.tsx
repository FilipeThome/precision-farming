import { Link, useParams } from 'react-router'

import { EvidencePack } from '@/features/compliance/components/EvidencePack'
import { TraceabilityChain } from '@/features/compliance/components/TraceabilityChain'
import { useTraceabilityQuery } from '@/features/compliance/queries'
import { useI18n } from '@/shared/i18n/useI18n'
import { queryError } from '@/shared/lib/queryError'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'
import { useUiStore } from '@/shared/ui/uiStore'

export function TraceabilityLotPage() {
  const { lotCode: rawLotCode } = useParams()
  const lotCode = rawLotCode ? decodeURIComponent(rawLotCode) : ''
  const farmId = useUiStore((s) => s.farmId)
  const traceability = useTraceabilityQuery(farmId)
  const { t } = useI18n()
  const err = queryError(traceability.error)

  const events = (traceability.data ?? []).filter((row) => row.lotCode === lotCode)

  return (
    <section>
      <PageHeader
        title={t('compliance.lot.title', { lotCode: lotCode || '—' })}
        description={t('compliance.lot.description')}
      />
      <Link
        to="/compliance"
        className="mb-4 inline-flex text-sm font-medium text-pf-teal hover:underline print-hide"
      >
        {t('compliance.lot.back')}
      </Link>
      <QueryPageState
        isLoading={traceability.isLoading}
        isError={traceability.isError}
        errorMessage={err.message}
        correlationId={err.correlationId}
        isEmpty={!traceability.isLoading && events.length === 0}
        emptyTitle={t('compliance.lot.emptyTitle')}
        emptyDescription={t('compliance.lot.emptyDescription')}
        onRetry={() => void traceability.refetch()}
      >
        <TraceabilityChain events={events} />
      </QueryPageState>
      {lotCode ? <EvidencePack lotCode={lotCode} /> : null}
    </section>
  )
}
