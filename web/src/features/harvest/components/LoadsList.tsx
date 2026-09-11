import { Truck } from 'lucide-react'
import { Link } from 'react-router'

import { useDispatchLoad } from '@/features/harvest/queries'
import type { LogisticsLoad } from '@/shared/api/types'
import { useCanManageFarmOps } from '@/shared/auth/roles'
import { useI18n } from '@/shared/i18n/useI18n'
import { queryError } from '@/shared/lib/queryError'
import { useFormat } from '@/shared/lib/useFormat'
import { Button } from '@/shared/ui/Button'
import { Card } from '@/shared/ui/Card'
import { EntityTile } from '@/shared/ui/EntityTile'
import { FreshnessChip } from '@/shared/ui/FreshnessChip'
import { StatusBadge } from '@/shared/ui/StatusBadge'

type Props = { loads: LogisticsLoad[]; farmId: string | null; isLoading: boolean; isError: boolean }

const ORDER: Record<string, number> = { QUEUED: 0, PLANNED: 0, DISPATCHED: 1, IN_TRANSIT: 1, IN_PROGRESS: 1 }

export function LoadsList({ loads, farmId, isLoading, isError }: Props) {
  const { t } = useI18n()
  const { number, label } = useFormat()
  const canManage = useCanManageFarmOps()
  const dispatch = useDispatchLoad()
  const err = dispatch.error ? queryError(dispatch.error) : null

  const visible = [...loads]
    .sort((a, b) => (ORDER[(a.status ?? '').toUpperCase()] ?? 2) - (ORDER[(b.status ?? '').toUpperCase()] ?? 2))
    .slice(0, 8)
  const detailHref = `/harvest/detail?tab=logistics${farmId ? `&farm=${encodeURIComponent(farmId)}` : ''}`

  return (
    <Card className="flex flex-col gap-2">
      <div className="flex items-center justify-between">
        <h2 className="flex items-center gap-1.5 font-display text-base font-bold">
          <Truck className="h-4 w-4 text-ag-t-600" aria-hidden />
          {t('postHarvest.loads.title')}
        </h2>
        <Link to={detailHref} className="text-xs font-semibold text-ag-t-700 hover:underline">
          {t('postHarvest.seeAll')}
        </Link>
      </div>
      {isLoading ? (
        <div className="h-24 animate-pulse rounded-[10px] bg-ag-n-100" aria-busy="true" />
      ) : isError ? (
        <p className="text-sm text-ag-crit" role="alert">
          {t('common.loadError')}
        </p>
      ) : visible.length === 0 ? (
        <p className="text-sm text-ag-n-600" role="status">
          {t('harvest.logistics.emptyTitle')}
        </p>
      ) : (
        <ul className="flex flex-col">
          {visible.map((load) => {
            const status = (load.status ?? '').toUpperCase()
            return (
              <li key={load.id} className="flex items-center gap-2.5 border-b border-ag-n-100 py-2 last:border-b-0">
                <EntityTile kind="truck" size="sm" tone={status === 'QUEUED' ? 'neutral' : 'teal'} />
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <b className="truncate text-[13px]">
                      {load.truckPlate ? t('harvest.logistics.truck', { plate: load.truckPlate }) : label(load.id)}
                    </b>
                    <StatusBadge value={load.status ?? 'UNKNOWN'} />
                  </div>
                  <div className="flex flex-wrap items-center gap-1.5 text-xs text-ag-n-600">
                    {load.tons != null ? <span className="font-mono">{number(load.tons, 1)} t</span> : null}
                    {load.destination ? <span>· {load.destination}</span> : null}
                    <FreshnessChip at={load.dispatchedAt} />
                  </div>
                </div>
                {status === 'QUEUED' && canManage ? (
                  <Button size="sm" variant="secondary" disabled={dispatch.isPending} onClick={() => dispatch.mutate(load.id)}>
                    {t('harvest.logistics.dispatch')}
                  </Button>
                ) : null}
              </li>
            )
          })}
        </ul>
      )}
      {err ? (
        <p className="text-xs text-ag-crit" role="alert">
          {err.message}
        </p>
      ) : null}
    </Card>
  )
}
