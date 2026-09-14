import { AlertInspector } from '@/features/alerts/components/AlertInspector'
import { useAckAlertMutation, useAlertsQuery } from '@/features/alerts/queries'
import type { MessageKey } from '@/shared/i18n/useI18n'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { queryError } from '@/shared/lib/queryError'
import { useSearchParam } from '@/shared/lib/useSearchParam'
import { useSelectedId } from '@/shared/lib/useSelectedId'
import { Button } from '@/shared/ui/Button'
import { Card } from '@/shared/ui/Card'
import { DetailDrawer } from '@/shared/ui/DetailDrawer'
import { EntityTile } from '@/shared/ui/EntityTile'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'
import { StatusBadge } from '@/shared/ui/StatusBadge'
import { useUiStore } from '@/shared/ui/uiStore'

const SEVERITIES = ['CRITICAL', 'WARNING', 'INFO'] as const
type Severity = (typeof SEVERITIES)[number]

const FILTER_KEYS: Record<Severity | 'ALL', MessageKey> = {
  ALL: 'alerts.filter.all',
  CRITICAL: 'alerts.filter.CRITICAL',
  WARNING: 'alerts.filter.WARNING',
  INFO: 'alerts.filter.INFO',
}

export function AlertsPage() {
  const farmId = useUiStore((s) => s.farmId)
  const alerts = useAlertsQuery(farmId)
  const ack = useAckAlertMutation()
  const err = queryError(alerts.error)
  const ackErr = ack.error ? queryError(ack.error) : null
  const { t } = useI18n()
  const { label, dateTime } = useFormat()
  const [severity, setSeverity] = useSearchParam('severity')
  const { selectedId, setSelectedId } = useSelectedId()
  const activeSeverity = SEVERITIES.includes(severity as Severity) ? (severity as Severity) : null
  const visible = (alerts.data ?? []).filter(
    (alert) => !activeSeverity || alert.severity === activeSeverity,
  )
  const selected = (alerts.data ?? []).find((alert) => alert.id === selectedId)

  return (
    <section>
      <PageHeader title={t('alerts.title')} description={t('alerts.description')} />
      {ackErr ? (
        <p className="mb-3 text-sm text-red-800" role="alert">
          {ackErr.message}
        </p>
      ) : null}
      <div className="mb-4 flex flex-wrap gap-2" role="group" aria-label={t('alerts.filter.all')}>
        {(['ALL', ...SEVERITIES] as const).map((value) => (
          <Button
            key={value}
            variant={(value === 'ALL' && !activeSeverity) || value === activeSeverity ? 'primary' : 'secondary'}
            aria-pressed={(value === 'ALL' && !activeSeverity) || value === activeSeverity}
            onClick={() => setSeverity(value === 'ALL' ? null : value)}
          >
            {t(FILTER_KEYS[value])}
          </Button>
        ))}
      </div>
      <QueryPageState
        isLoading={alerts.isLoading}
        isError={alerts.isError}
        errorMessage={err.message}
        correlationId={err.correlationId}
        isEmpty={!alerts.isLoading && visible.length === 0}
        emptyTitle={t('alerts.emptyTitle')}
        emptyDescription={t('alerts.emptyDescription')}
        onRetry={() => void alerts.refetch()}
      >
        <div className="flex flex-col gap-3">
          {visible.map((alert) => {
            return (
              <Card
                key={alert.id}
                className={`flex flex-wrap items-start justify-between gap-3 ${
                  alert.id === selectedId ? 'ring-2 ring-pf-teal' : ''
                }`}
              >
                <button
                  type="button"
                  className="flex min-w-0 flex-1 items-start gap-3 rounded-[12px] text-left focus-visible:outline focus-visible:ring-2 focus-visible:ring-pf-teal"
                  aria-pressed={alert.id === selectedId}
                  onClick={() => setSelectedId(alert.id)}
                >
                  <EntityTile
                    kind={alert.entityType === 'MACHINE' ? 'machine' : 'alert'}
                    size="sm"
                    tone={alert.severity === 'CRITICAL' ? 'crit' : 'warn'}
                    label={label(alert.title)}
                  />
                  <div>
                    <div className="flex flex-wrap items-center gap-2">
                      <h2 className="font-semibold text-pf-green">{label(alert.title)}</h2>
                      <StatusBadge value={alert.severity} />
                      <StatusBadge value={alert.status} />
                    </div>
                    <p className="mt-1 text-sm text-pf-muted">{label(alert.message)}</p>
                    <p className="mt-1 text-xs text-pf-muted">{dateTime(alert.createdAt)}</p>
                  </div>
                </button>
                {alert.status === 'OPEN' ? (
                  <Button disabled={ack.isPending} onClick={() => ack.mutate(alert.id)}>
                    {t('alerts.ack')}
                  </Button>
                ) : null}
              </Card>
            )
          })}
        </div>
      </QueryPageState>
      <DetailDrawer
        open={Boolean(selectedId)}
        title={selected ? label(selected.title) : t('inspector.notFound')}
        subtitle={selected ? undefined : t('inspector.notFoundHint')}
        onClose={() => setSelectedId(null)}
      >
        {selected ? <AlertInspector alert={selected} /> : null}
      </DetailDrawer>
    </section>
  )
}
