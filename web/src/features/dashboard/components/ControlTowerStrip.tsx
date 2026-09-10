import type { ReactNode } from 'react'
import { Link } from 'react-router'

import type { Alert, Machine, Operation } from '@/shared/api/types'
import { farmPhoto, machinePhoto } from '@/shared/demo/media'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { EntityPhoto } from '@/shared/ui/EntityPhoto'
import { StatusBadge } from '@/shared/ui/StatusBadge'

type Props = {
  alerts: Alert[]
  machines: Machine[]
  operations: Operation[]
}

function rankMachine(status: string): number {
  if (status === 'OPERATING') return 0
  if (status === 'IDLE') return 1
  return 2
}

export function ControlTowerStrip({ alerts, machines, operations }: Props) {
  const { t } = useI18n()
  const { label } = useFormat()

  const openAlerts = [...alerts]
    .filter((alert) => alert.status === 'OPEN')
    .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
    .slice(0, 5)
  const fleet = [...machines]
    .filter((machine) => machine.status === 'OPERATING' || machine.status === 'IDLE')
    .sort((a, b) => rankMachine(a.status) - rankMachine(b.status))
    .slice(0, 5)
  const running = operations.filter((op) => op.status === 'IN_PROGRESS').slice(0, 5)

  return (
    <div className="mt-6 grid gap-4 xl:grid-cols-3">
      <TowerColumn title={t('dashboard.tower.alerts')} empty={t('dashboard.tower.empty')}>
        {openAlerts.map((alert) => (
          <TowerChip
            key={alert.id}
            to={`/alerts?selected=${alert.id}`}
            photo={
              alert.entityType === 'MACHINE' && alert.entityId
                ? machinePhoto(alert.entityId)
                : farmPhoto(alert.farmId)
            }
            title={label(alert.title)}
            status={alert.severity}
          />
        ))}
      </TowerColumn>
      <TowerColumn title={t('dashboard.tower.fleet')} empty={t('dashboard.tower.empty')}>
        {fleet.map((machine) => (
          <TowerChip
            key={machine.id}
            to={`/machines?selected=${machine.id}`}
            photo={machinePhoto(machine.id, machine.type)}
            title={label(machine.id, machine.name)}
            status={machine.status}
          />
        ))}
      </TowerColumn>
      <TowerColumn title={t('dashboard.tower.ops')} empty={t('dashboard.tower.empty')}>
        {running.map((op) => (
          <TowerChip
            key={op.id}
            to={`/operations?selected=${op.id}`}
            photo={machinePhoto(op.machineId) ?? farmPhoto(op.farmId)}
            title={label(op.type)}
            status={op.status}
          />
        ))}
      </TowerColumn>
    </div>
  )
}

function TowerColumn({
  title,
  empty,
  children,
}: {
  title: string
  empty: string
  children: ReactNode
}) {
  const items = Array.isArray(children) ? children : [children]
  const hasItems = items.filter(Boolean).length > 0
  return (
    <section className="rounded-[12px] border border-pf-border bg-white p-3">
      <h3 className="mb-2 text-sm font-semibold text-pf-green">{title}</h3>
      <div className="flex flex-col gap-2">
        {hasItems ? children : <p className="text-sm text-pf-muted">{empty}</p>}
      </div>
    </section>
  )
}

function TowerChip({
  to,
  photo,
  title,
  status,
}: {
  to: string
  photo?: string
  title: string
  status: string
}) {
  return (
    <Link
      to={to}
      className="flex items-center gap-2 rounded-[12px] border border-transparent px-1 py-1 hover:border-pf-teal focus-visible:outline focus-visible:ring-2 focus-visible:ring-pf-teal"
    >
      <EntityPhoto variant="thumb" src={photo} alt={title} />
      <span className="min-w-0 flex-1 truncate text-sm text-pf-green">{title}</span>
      <StatusBadge value={status} />
    </Link>
  )
}
