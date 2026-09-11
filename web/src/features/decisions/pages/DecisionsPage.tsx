import { useNavigate, useParams } from 'react-router'

import { DecisionDetail } from '@/features/decisions/components/DecisionDetail'
import { DecisionList } from '@/features/decisions/components/DecisionList'
import { decisionsHref } from '@/features/decisions/components/DecisionRow'
import { useDecisionSources } from '@/features/decisions/queries'
import type { DecisionFilter } from '@/features/decisions/selectors'
import { useOperationsQuery } from '@/features/operations/queries'
import { useI18n } from '@/shared/i18n/useI18n'
import { queryError } from '@/shared/lib/queryError'
import { useSearchParam } from '@/shared/lib/useSearchParam'
import { Card } from '@/shared/ui/Card'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'
import { useUiStore } from '@/shared/ui/uiStore'

const FILTERS: DecisionFilter[] = ['pending', 'approved', 'all']

export function DecisionsPage() {
  const farmId = useUiStore((s) => s.farmId)
  const { id } = useParams<{ id?: string }>()
  const navigate = useNavigate()
  const { t } = useI18n()
  const [filterParam] = useSearchParam('status', true)
  const filter: DecisionFilter = FILTERS.includes(filterParam as DecisionFilter) ? (filterParam as DecisionFilter) : 'pending'

  const decisions = useDecisionSources(farmId)
  const operations = useOperationsQuery(farmId)
  const err = queryError(decisions.error)
  const selectedId = id ? decodeURIComponent(id) : null
  const selected = selectedId ? decisions.items.find((item) => item.id === selectedId) : undefined

  return (
    <section className="flex flex-col gap-3.5">
      <PageHeader title={t('decisions.title')} description={t('decisions.description')} />
      {decisions.partialError ? (
        <p className="text-xs text-ag-warn" role="status">
          {t('decisions.partialError')}
        </p>
      ) : null}
      <QueryPageState
        isLoading={decisions.isLoading}
        isError={decisions.isError}
        errorMessage={err.message}
        correlationId={err.correlationId}
        isEmpty={false}
        emptyTitle={t('decisions.empty')}
        onRetry={decisions.refetch}
      >
        <div className="grid gap-3.5 xl:grid-cols-[360px_1fr]">
          <DecisionList
            items={decisions.items}
            filter={filter}
            onFilterChange={(next) => {
              navigate(decisionsHref({ id: selectedId, farmId, status: next }), { replace: true })
            }}
            selectedId={selectedId}
          />
          {selected ? (
            <DecisionDetail item={selected} operations={operations.data ?? []} />
          ) : selectedId ? (
            <Card>
              <h2 className="font-display text-base font-bold">{t('inspector.notFound')}</h2>
              <p className="mt-1 text-sm text-ag-n-600">{t('inspector.notFoundHint')}</p>
            </Card>
          ) : (
            <Card className="grid min-h-[240px] place-items-center text-center">
              <div>
                <h2 className="font-display text-base font-bold">{t('decisions.selectTitle')}</h2>
                <p className="mt-1 text-sm text-ag-n-600">{t('decisions.selectHint')}</p>
              </div>
            </Card>
          )}
        </div>
      </QueryPageState>
    </section>
  )
}
