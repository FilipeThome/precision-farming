import { useAckAlertMutation, useAlertsQuery } from '@/features/alerts/queries'
import { useI18n } from '@/shared/i18n/useI18n'
import { formatDateTime } from '@/shared/lib/format'
import { queryError } from '@/shared/lib/queryError'
import { Button } from '@/shared/ui/Button'
import { Card } from '@/shared/ui/Card'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'
import { StatusBadge } from '@/shared/ui/StatusBadge'
import { useUiStore } from '@/shared/ui/uiStore'

export function AlertsPage() {
  const farmId = useUiStore((s) => s.farmId)
  const alerts = useAlertsQuery(farmId)
  const ack = useAckAlertMutation()
  const err = queryError(alerts.error)
  const ackErr = ack.error ? queryError(ack.error) : null
  const { t } = useI18n()

  return (
    <section>
      <PageHeader title={t('alerts.title')} description={t('alerts.description')} />
      {ackErr ? (
        <p className="mb-3 text-sm text-red-800" role="alert">
          {ackErr.message}
        </p>
      ) : null}
      <QueryPageState
        isLoading={alerts.isLoading}
        isError={alerts.isError}
        errorMessage={err.message}
        correlationId={err.correlationId}
        isEmpty={!alerts.isLoading && (alerts.data?.length ?? 0) === 0}
        emptyTitle={t('alerts.emptyTitle')}
        emptyDescription={t('alerts.emptyDescription')}
        onRetry={() => void alerts.refetch()}
      >
        <div className="flex flex-col gap-3">
          {(alerts.data ?? []).map((alert) => (
            <Card key={alert.id} className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <div className="flex flex-wrap items-center gap-2">
                  <h2 className="font-semibold text-pf-green">{alert.title}</h2>
                  <StatusBadge value={alert.severity} />
                  <StatusBadge value={alert.status} />
                </div>
                <p className="mt-1 text-sm text-pf-muted">{alert.message}</p>
                <p className="mt-1 text-xs text-pf-muted">{formatDateTime(alert.createdAt)}</p>
              </div>
              {alert.status === 'OPEN' ? (
                <Button disabled={ack.isPending} onClick={() => ack.mutate(alert.id)}>
                  Reconhecer
                </Button>
              ) : null}
            </Card>
          ))}
        </div>
      </QueryPageState>
    </section>
  )
}
