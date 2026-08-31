import { useFieldsQuery } from '@/features/fields/queries'
import { queryError } from '@/shared/lib/queryError'
import { FieldMap } from '@/shared/maps/FieldMap'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'
import { useUiStore } from '@/shared/ui/uiStore'

export function MapPage() {
  const farmId = useUiStore((s) => s.farmId)
  const fields = useFieldsQuery(farmId)
  const err = queryError(fields.error)

  return (
    <section className="flex h-full flex-col">
      <PageHeader
        title="Mapa"
        description="Polígonos dos talhões em satélite quando a chave do Google Maps estiver configurada."
      />
      <QueryPageState
        isLoading={fields.isLoading}
        isError={fields.isError}
        errorMessage={err.message}
        correlationId={err.correlationId}
        isEmpty={false}
        emptyTitle="Nenhum talhão para desenhar"
        onRetry={() => void fields.refetch()}
      >
        <FieldMap fields={fields.data ?? []} className="min-h-[560px] flex-1" />
      </QueryPageState>
    </section>
  )
}
