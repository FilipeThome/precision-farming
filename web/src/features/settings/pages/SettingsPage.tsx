import { useMeQuery } from '@/features/auth/queries'
import { useAuthStore } from '@/shared/auth/store'
import { queryError } from '@/shared/lib/queryError'
import { Card } from '@/shared/ui/Card'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'

export function SettingsPage() {
  const token = useAuthStore((s) => s.accessToken)
  const me = useMeQuery(Boolean(token))
  const err = queryError(me.error)

  return (
    <section>
      <PageHeader title="Configurações" description="Perfil autenticado via GET /api/v1/auth/me." />
      <QueryPageState
        isLoading={me.isLoading}
        isError={me.isError}
        errorMessage={err.message}
        correlationId={err.correlationId}
        isEmpty={!me.isLoading && !me.data}
        emptyTitle="Usuário não encontrado"
        emptyDescription="Não foi possível obter o perfil."
        onRetry={() => void me.refetch()}
      >
        {me.data ? (
          <Card className="max-w-lg">
            <dl className="grid grid-cols-[8rem_1fr] gap-2 text-sm">
              <dt className="text-pf-muted">Nome</dt>
              <dd>{me.data.name}</dd>
              <dt className="text-pf-muted">E-mail</dt>
              <dd>{me.data.email}</dd>
              <dt className="text-pf-muted">Papel</dt>
              <dd>{me.data.role}</dd>
              <dt className="text-pf-muted">ID</dt>
              <dd className="break-all">{me.data.id}</dd>
            </dl>
          </Card>
        ) : null}
      </QueryPageState>
    </section>
  )
}
