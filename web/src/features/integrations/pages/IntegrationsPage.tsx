import { ConnectorCard } from '@/features/integrations/components/ConnectorCard'
import { useIntegrationsQuery } from '@/features/integrations/queries'
import { useI18n } from '@/shared/i18n/useI18n'
import { queryError } from '@/shared/lib/queryError'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'

export function IntegrationsPage() {
  const integrations = useIntegrationsQuery()
  const err = queryError(integrations.error)
  const { t } = useI18n()

  return (
    <section>
      <PageHeader title={t('integrations.title')} description={t('integrations.description')} />
      <QueryPageState
        isLoading={integrations.isLoading}
        isError={integrations.isError}
        errorMessage={err.message}
        correlationId={err.correlationId}
        isEmpty={!integrations.isLoading && (integrations.data?.length ?? 0) === 0}
        emptyTitle={t('integrations.emptyTitle')}
        emptyDescription={t('integrations.emptyDescription')}
        onRetry={() => void integrations.refetch()}
      >
        <div className="grid gap-3 md:grid-cols-2">
          {(integrations.data ?? []).map((connector) => (
            <ConnectorCard key={connector.name} connector={connector} />
          ))}
        </div>
      </QueryPageState>
    </section>
  )
}
