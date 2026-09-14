import type { DecisionItem } from '@/features/decisions/model'
import { filterDecisions, sortByPriority, type DecisionFilter } from '@/features/decisions/selectors'
import { useI18n } from '@/shared/i18n/useI18n'
import { Card } from '@/shared/ui/Card'

import { DecisionRow } from './DecisionRow'

type Props = {
  items: DecisionItem[]
  filter: DecisionFilter
  onFilterChange: (next: DecisionFilter) => void
  selectedId: string | null
}

const FILTERS: DecisionFilter[] = ['pending', 'approved', 'all']

export function DecisionList({ items, filter, onFilterChange, selectedId }: Props) {
  const { t } = useI18n()
  const visible = sortByPriority(filterDecisions(items, filter))

  return (
    <Card className="flex min-h-0 flex-col gap-2 p-2.5">
      <div className="flex gap-1 rounded-[10px] bg-ag-n-100 p-1" role="tablist" aria-label={t('decisions.filter.label')}>
        {FILTERS.map((f) => (
          <button
            key={f}
            type="button"
            role="tab"
            aria-selected={filter === f}
            onClick={() => onFilterChange(f)}
            className={`flex-1 rounded-[8px] px-2 py-1.5 text-xs font-semibold transition ${
              filter === f ? 'bg-ag-n-0 text-ag-g-800 shadow-[0_1px_2px_rgba(20,30,25,0.06)]' : 'text-ag-n-600 hover:text-ag-n-900'
            }`}
          >
            {t(`decisions.filter.${f}` as const)}
            <span className="ml-1 font-mono text-[10px] text-ag-n-500">{filterDecisions(items, f).length}</span>
          </button>
        ))}
      </div>
      {visible.length === 0 ? (
        <p className="px-2 py-6 text-center text-sm text-ag-n-600" role="status">
          {t('decisions.empty')}
        </p>
      ) : (
        <ul className="flex flex-col gap-0.5 overflow-y-auto">
          {visible.map((item) => (
            <DecisionRow key={item.id} item={item} selected={item.id === selectedId} status={filter} />
          ))}
        </ul>
      )}
    </Card>
  )
}
