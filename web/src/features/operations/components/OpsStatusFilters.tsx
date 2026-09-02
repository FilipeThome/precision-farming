import type { MessageKey } from '@/shared/i18n/useI18n'
import { useI18n } from '@/shared/i18n/useI18n'
import { Button } from '@/shared/ui/Button'

export const OPS_STATUSES = ['PLANNED', 'IN_PROGRESS', 'PAUSED', 'COMPLETED'] as const
export type OpsStatus = (typeof OPS_STATUSES)[number]
export type OpsStatusFilter = OpsStatus | 'ALL'

const FILTER_KEYS: Record<OpsStatusFilter, MessageKey> = {
  ALL: 'operations.filter.all',
  PLANNED: 'operations.filter.PLANNED',
  IN_PROGRESS: 'operations.filter.IN_PROGRESS',
  PAUSED: 'operations.filter.PAUSED',
  COMPLETED: 'operations.filter.COMPLETED',
}

type Props = {
  value: OpsStatusFilter
  onChange: (value: OpsStatusFilter) => void
}

export function OpsStatusFilters({ value, onChange }: Props) {
  const { t } = useI18n()
  const options: OpsStatusFilter[] = ['ALL', ...OPS_STATUSES]

  return (
    <div className="mb-4 flex flex-wrap gap-2" role="group" aria-label={t('operations.filter.all')}>
      {options.map((status) => (
        <Button
          key={status}
          variant={value === status ? 'primary' : 'secondary'}
          aria-pressed={value === status}
          onClick={() => onChange(status)}
        >
          {t(FILTER_KEYS[status])}
        </Button>
      ))}
    </div>
  )
}
