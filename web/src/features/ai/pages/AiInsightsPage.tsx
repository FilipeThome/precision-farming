import { useInsightsQuery } from '@/features/ai/queries'
import { useI18n } from '@/shared/i18n/useI18n'
import { formatDateTime, formatPercent } from '@/shared/lib/format'
import { queryError } from '@/shared/lib/queryError'
import { Card } from '@/shared/ui/Card'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'
import { useUiStore } from '@/shared/ui/uiStore'

export function AiInsightsPage() {
  const farmId = useUiStore((s) => s.farmId)
  const insights = useInsightsQuery(farmId)
  const err = queryError(insights.error)
  const { t } = useI18n()

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
            <Card key={item.id} className="flex flex-col gap-2">
              <div className="flex items-start justify-between gap-2">
                <h2 className="font-semibold text-pf-green">{item.type}</h2>
                <span className="rounded-full bg-pf-teal/15 px-2 py-0.5 text-xs font-medium text-pf-teal">
                  Demo model
                </span>
              </div>
              <p className="text-sm text-pf-muted">
                {item.model} · v{item.modelVersion} · {formatDateTime(item.generatedAt)}
                {item.demo ? ' · demo' : ''}
              </p>
              <p className="text-sm">
                Score {formatPercent(item.score)} · confiança {formatPercent(item.confidence)}
              </p>
              <ul className="list-disc pl-5 text-sm text-pf-muted">
                {item.explanation.map((line) => (
                  <li key={line}>{line}</li>
                ))}
              </ul>
            </Card>
          ))}
        </div>
      </QueryPageState>
    </section>
  )
}
