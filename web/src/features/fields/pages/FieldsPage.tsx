import { useFieldsQuery } from '@/features/fields/queries'
import { formatNumber } from '@/shared/lib/format'
import { queryError } from '@/shared/lib/queryError'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'
import { useUiStore } from '@/shared/ui/uiStore'

export function FieldsPage() {
  const farmId = useUiStore((s) => s.farmId)
  const fields = useFieldsQuery(farmId)
  const err = queryError(fields.error)

  return (
    <section>
      <PageHeader title="Talhões" description="Talhões e culturas associadas." />
      <QueryPageState
        isLoading={fields.isLoading}
        isError={fields.isError}
        errorMessage={err.message}
        correlationId={err.correlationId}
        isEmpty={!fields.isLoading && (fields.data?.length ?? 0) === 0}
        emptyTitle="Nenhum talhão encontrado"
        emptyDescription="Ajuste o filtro de fazenda ou cadastre talhões no backend."
        onRetry={() => void fields.refetch()}
      >
        <div className="overflow-hidden rounded-[12px] border border-pf-border bg-white">
          <table className="w-full text-left text-sm">
            <thead className="bg-pf-bg text-pf-muted">
              <tr>
                <th className="px-4 py-3 font-medium">Nome</th>
                <th className="px-4 py-3 font-medium">Cultura</th>
                <th className="px-4 py-3 font-medium">Variedade</th>
                <th className="px-4 py-3 font-medium">Área (ha)</th>
              </tr>
            </thead>
            <tbody>
              {(fields.data ?? []).map((field) => (
                <tr key={field.id} className="border-t border-pf-border">
                  <td className="px-4 py-3 font-medium text-pf-green">{field.name}</td>
                  <td className="px-4 py-3">{field.crop}</td>
                  <td className="px-4 py-3">{field.variety ?? '—'}</td>
                  <td className="px-4 py-3">{formatNumber(Number(field.areaHa), 1)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </QueryPageState>
    </section>
  )
}
