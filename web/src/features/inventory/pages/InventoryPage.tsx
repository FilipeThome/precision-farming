import { useInventoryQuery } from '@/features/inventory/queries'
import { useI18n } from '@/shared/i18n/useI18n'
import { formatNumber } from '@/shared/lib/format'
import { queryError } from '@/shared/lib/queryError'
import { PageHeader } from '@/shared/ui/PageHeader'
import { QueryPageState } from '@/shared/ui/QueryPageState'
import { useUiStore } from '@/shared/ui/uiStore'

export function InventoryPage() {
  const farmId = useUiStore((s) => s.farmId)
  const inventory = useInventoryQuery(farmId)
  const err = queryError(inventory.error)
  const { t } = useI18n()

  return (
    <section>
      <PageHeader title={t('inventory.title')} description={t('inventory.description')} />
      <QueryPageState
        isLoading={inventory.isLoading}
        isError={inventory.isError}
        errorMessage={err.message}
        correlationId={err.correlationId}
        isEmpty={!inventory.isLoading && (inventory.data?.length ?? 0) === 0}
        emptyTitle={t('inventory.emptyTitle')}
        emptyDescription={t('inventory.emptyDescription')}
        onRetry={() => void inventory.refetch()}
      >
        <div className="overflow-hidden rounded-[12px] border border-pf-border bg-white">
          <table className="w-full text-left text-sm">
            <thead className="bg-pf-bg text-pf-muted">
              <tr>
                <th className="px-4 py-3 font-medium">Item</th>
                <th className="px-4 py-3 font-medium">Categoria</th>
                <th className="px-4 py-3 font-medium">Quantidade</th>
                <th className="px-4 py-3 font-medium">Reservado</th>
              </tr>
            </thead>
            <tbody>
              {(inventory.data ?? []).map((item) => (
                <tr key={item.id} className="border-t border-pf-border">
                  <td className="px-4 py-3 font-medium text-pf-green">{item.name}</td>
                  <td className="px-4 py-3">{item.category}</td>
                  <td className="px-4 py-3">
                    {formatNumber(Number(item.quantity), 1)} {item.unit}
                  </td>
                  <td className="px-4 py-3">
                    {formatNumber(Number(item.reserved), 1)} {item.unit}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </QueryPageState>
    </section>
  )
}
