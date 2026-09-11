import { Warehouse } from 'lucide-react'
import { Link } from 'react-router'

import { unitOccupancyPct } from '@/features/harvest/model/postHarvestKpis'
import type { StorageUnit } from '@/shared/api/types'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { Card } from '@/shared/ui/Card'
import { EntityTile } from '@/shared/ui/EntityTile'

type Props = { units: StorageUnit[]; farmId: string | null; isLoading: boolean; isError: boolean }

export function SiloCards({ units, farmId, isLoading, isError }: Props) {
  const { t } = useI18n()
  const { number, label } = useFormat()
  const detailHref = `/harvest/detail?tab=storage${farmId ? `&farm=${encodeURIComponent(farmId)}` : ''}`

  return (
    <Card className="flex flex-col gap-2">
      <div className="flex items-center justify-between">
        <h2 className="flex items-center gap-1.5 font-display text-base font-bold">
          <Warehouse className="h-4 w-4 text-ag-t-600" aria-hidden />
          {t('postHarvest.silos.title')}
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
      ) : units.length === 0 ? (
        <p className="text-sm text-ag-n-600" role="status">
          {t('harvest.storage.emptyTitle')}
        </p>
      ) : (
        <ul className="grid gap-2 sm:grid-cols-2">
          {units.slice(0, 6).map((unit) => {
            const pct = unitOccupancyPct(unit)
            const tone = pct == null ? 'bg-ag-n-300' : pct >= 90 ? 'bg-ag-crit' : pct >= 70 ? 'bg-ag-warn' : 'bg-ag-t-500'
            return (
              <li key={unit.id} className="flex flex-col gap-1.5 rounded-[10px] border border-ag-n-200 p-2.5">
                <div className="flex items-center gap-2">
                  <EntityTile kind="storage" size="sm" tone="brand" />
                  <div className="min-w-0">
                    <b className="block truncate text-[13px]">{unit.name ?? label(unit.id)}</b>
                    <span className="text-[11px] text-ag-n-600">{label(unit.type)}</span>
                  </div>
                  <span className="ml-auto font-mono text-sm font-semibold tnum">{pct == null ? '—' : `${pct}%`}</span>
                </div>
                <div
                  className="h-1.5 overflow-hidden rounded-full bg-ag-n-100"
                  role="progressbar"
                  aria-valuemin={0}
                  aria-valuemax={100}
                  aria-valuenow={pct ?? undefined}
                  aria-label={unit.name ?? unit.id}
                >
                  <div className={`h-full rounded-full ${tone}`} style={{ width: `${pct ?? 0}%` }} />
                </div>
                <span className="text-[11px] text-ag-n-600">
                  {t('harvest.storage.occupancy', { used: number(unit.usedT ?? 0, 0), capacity: number(unit.capacityT ?? 0, 0) })}
                </span>
              </li>
            )
          })}
        </ul>
      )}
    </Card>
  )
}
