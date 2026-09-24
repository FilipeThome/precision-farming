import { useMemo } from 'react'

import { useMoaRotationQueries, usePrescriptionsQuery } from '@/features/agronomy/queries'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { queryError } from '@/shared/lib/queryError'
import { EntityCard } from '@/shared/ui/EntityCard'
import { QueryPageState } from '@/shared/ui/QueryPageState'
import { SimulationNotice } from '@/shared/ui/SimulationNotice'

type Props = { farmId?: string | null }

export function MoaWarningsPanel({ farmId }: Props) {
  const { t } = useI18n()
  const { label } = useFormat()
  const prescriptions = usePrescriptionsQuery(farmId)
  const fieldIds = useMemo(() => {
    const ids = new Set<string>()
    for (const row of prescriptions.data ?? []) {
      if (row.fieldId) ids.add(row.fieldId)
    }
    return [...ids]
  }, [prescriptions.data])

  const rotations = useMoaRotationQueries(fieldIds, {
    enabled: prescriptions.isSuccess && fieldIds.length > 0,
  })

  const loading =
    prescriptions.isLoading ||
    (fieldIds.length > 0 && rotations.some((q) => q.isLoading))
  const firstError = prescriptions.error ?? rotations.find((q) => q.error)?.error
  const err = queryError(firstError)
  const warnings = rotations
    .map((q) => q.data)
    .filter((row): row is NonNullable<typeof row> => Boolean(row?.warning))

  return (
    <QueryPageState
      isLoading={loading}
      isError={Boolean(firstError)}
      errorMessage={err.message}
      correlationId={err.correlationId}
      isEmpty={!loading && !firstError && warnings.length === 0}
      emptyTitle={t('agronomy.moa.emptyTitle')}
      emptyDescription={t('agronomy.moa.emptyDescription')}
      onRetry={() => {
        void prescriptions.refetch()
        for (const q of rotations) void q.refetch()
      }}
    >
      <div className="grid gap-3 md:grid-cols-2">
        {warnings.map((row) => (
          <EntityCard
            key={row.fieldId}
            title={label(row.fieldId)}
            subtitle={label(row.moaGroup)}
            meta={t('agronomy.moa.warning')}
            kind="recommendation"
          >
            <SimulationNotice simulation={row.simulation === true} className="mt-2" />
          </EntityCard>
        ))}
      </div>
    </QueryPageState>
  )
}
