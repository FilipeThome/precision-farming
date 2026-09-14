import { Link } from 'react-router'

import type { Machine } from '@/shared/api/types'
import { useI18n } from '@/shared/i18n/useI18n'
import { farmHref, inspectHref } from '@/shared/lib/useFarmFromSearch'
import { useFormat } from '@/shared/lib/useFormat'
import { EntityTile, type EntityTone } from '@/shared/ui/EntityTile'
import { StatusBadge } from '@/shared/ui/StatusBadge'

import { TowerCard } from './TowerCard'

type Props = {
  machines: Machine[]
  farmId: string | null
  isLoading: boolean
  isError: boolean
  errorMessage?: string
  onRetry: () => void
}

const MAX_VISIBLE = 6

function rank(status: string): number {
  if (status === 'OPERATING') return 0
  if (status === 'IDLE') return 1
  if (status === 'MAINTENANCE') return 2
  return 3
}

function toneOf(status: string): EntityTone {
  if (status === 'OPERATING') return 'teal'
  if (status === 'MAINTENANCE') return 'warn'
  return 'brand'
}

export function visibleFleet(machines: Machine[]): Machine[] {
  return [...machines].sort((a, b) => rank(a.status) - rank(b.status)).slice(0, MAX_VISIBLE)
}

/** Fleet list from GET /machines only — no per-machine metrics fetches. */
export function FleetList({ machines, farmId, isLoading, isError, errorMessage, onRetry }: Props) {
  const { t } = useI18n()
  const { label } = useFormat()
  const visible = visibleFleet(machines)
  const available = machines.filter((m) => m.status === 'OPERATING' || m.status === 'IDLE').length

  return (
    <TowerCard
      title={t('tower.fleet.title')}
      aside={
        machines.length > 0 ? (
          <span className="text-xs text-ag-n-600">{t('tower.fleet.available', { n: available, m: machines.length })}</span>
        ) : null
      }
      isLoading={isLoading}
      isError={isError}
      errorMessage={errorMessage}
      onRetry={onRetry}
      isEmpty={machines.length === 0}
      emptyText={t('tower.fleet.empty')}
      bodyClassName="px-1.5 pb-1.5"
    >
      <ul className="flex flex-col">
        {visible.map((machine) => (
          <li key={machine.id} className="border-t border-ag-n-100 first:border-t-0">
            <Link
              to={inspectHref('/machines', machine.id, machine.farmId ?? farmId)}
              className="flex items-center gap-2.5 rounded-[10px] px-2 py-2 hover:bg-ag-n-50"
            >
              <EntityTile kind="machine" machineType={machine.type} tone={toneOf(machine.status)} size="sm" />
              <span className="flex min-w-0 flex-1 flex-col gap-0.5">
                <span className="flex items-center justify-between gap-2">
                  <b className="truncate text-[13px]">{label(machine.id, machine.name)}</b>
                  <StatusBadge value={machine.status} />
                </span>
                <span className="truncate text-xs text-ag-n-600">
                  {label(machine.type)} · {machine.manufacturer} {machine.model}
                </span>
              </span>
            </Link>
          </li>
        ))}
      </ul>
      {machines.length > MAX_VISIBLE ? (
        <Link to={farmHref('/machines', farmId)} className="mt-1 block px-2 py-1 text-xs font-semibold text-ag-t-700 hover:underline">
          {t('tower.fleet.seeAll')}
        </Link>
      ) : null}
    </TowerCard>
  )
}
