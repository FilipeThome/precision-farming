import { useMeQuery } from '@/features/auth/queries'
import { useAuthStore } from '@/shared/auth/store'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { queryError } from '@/shared/lib/queryError'
import { Card } from '@/shared/ui/Card'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'

export function SettingsPage() {
  const token = useAuthStore((s) => s.accessToken)
  const me = useMeQuery(Boolean(token))
  const err = queryError(me.error)
  const { t } = useI18n()
  const { label } = useFormat()

  return (
    <section>
      <PageHeader title={t('settings.title')} description={t('settings.description')} />
      <QueryPageState
        isLoading={me.isLoading}
        isError={me.isError}
        errorMessage={err.message}
        correlationId={err.correlationId}
        isEmpty={!me.isLoading && !me.data}
        emptyTitle={t('settings.emptyTitle')}
        emptyDescription={t('settings.emptyDescription')}
        onRetry={() => void me.refetch()}
      >
        {me.data ? (
          <Card className="max-w-lg">
            <dl className="grid grid-cols-[8rem_1fr] gap-2 text-sm">
              <dt className="text-pf-muted">{t('settings.name')}</dt>
              <dd>{me.data.name}</dd>
              <dt className="text-pf-muted">{t('settings.email')}</dt>
              <dd>{me.data.email}</dd>
              <dt className="text-pf-muted">{t('settings.role')}</dt>
              <dd>{label(me.data.role)}</dd>
              <dt className="text-pf-muted">{t('settings.id')}</dt>
              <dd className="break-all">{me.data.id}</dd>
            </dl>
          </Card>
        ) : null}
      </QueryPageState>
    </section>
  )
}
