import { InsightInspector } from '@/features/ai/components/InsightInspector'
import { useInsightsQuery } from '@/features/ai/queries'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { queryError } from '@/shared/lib/queryError'
import { useSelectedId } from '@/shared/lib/useSelectedId'
import { Card } from '@/shared/ui/Card'
import { DetailDrawer } from '@/shared/ui/DetailDrawer'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'
import { useUiStore } from '@/shared/ui/uiStore'

export function AiInsightsPage() {
  const farmId = useUiStore((s) => s.farmId)
  const insights = useInsightsQuery(farmId)
  const err = queryError(insights.error)
  const { t } = useI18n()
  const { dateTime, percent, label } = useFormat()
  const { selectedId, setSelectedId } = useSelectedId()
  const selected = (insights.data ?? []).find((item) => item.id === selectedId)

  return (
    <section>
      <PageHeader title={t('ai.title')} description={t('ai.description')} />
      <QueryPageState
        isLoading={insights.isLoading}
        isError={insights.isError}
        errorMessage={err.message}
        correlationId={err.correlationId}
        isEmpty={!insights.isLoading && (insights.data?.length ?? 0) === 0}
        emptyTitle={t('ai.emptyTitle')}
        emptyDescription={t('ai.emptyDescription')}
        onRetry={() => void insights.refetch()}
      >
        <div className="grid gap-4 lg:grid-cols-2">
          {(insights.data ?? []).map((item) => (
            <Card
              key={item.id}
              className={`flex cursor-pointer flex-col gap-2 transition hover:border-pf-teal ${
                item.id === selectedId ? 'ring-2 ring-pf-teal' : ''
              }`}
              role="button"
              tabIndex={0}
              aria-pressed={item.id === selectedId}
              onClick={() => setSelectedId(item.id)}
              onKeyDown={(event) => {
                if (event.key === 'Enter' || event.key === ' ') {
                  event.preventDefault()
                  setSelectedId(item.id)
                }
              }}
            >
              <div className="flex items-start justify-between gap-2">
                <h2 className="font-semibold text-pf-green">{label(item.type)}</h2>
                <span className="rounded-full bg-pf-teal/15 px-2 py-0.5 text-xs font-medium text-pf-teal">
                  {t('ai.demoModel')}
                </span>
              </div>
              <p className="text-sm text-pf-muted">
                {item.model} · v{item.modelVersion} · {dateTime(item.generatedAt)}
              </p>
              <p className="text-sm">
                {t('ai.scoreConfidence', {
                  score: percent(item.score),
                  confidence: percent(item.confidence),
                })}
              </p>
            </Card>
          ))}
        </div>
      </QueryPageState>
      <DetailDrawer
        open={Boolean(selectedId)}
        title={selected ? label(selected.type) : t('inspector.notFound')}
        subtitle={selected ? undefined : t('inspector.notFoundHint')}
        onClose={() => setSelectedId(null)}
      >
        {selected ? <InsightInspector insight={selected} /> : null}
      </DetailDrawer>
    </section>
  )
}
