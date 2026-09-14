import { useMemo } from 'react'

import { usePrescriptionsQuery, useRecommendationsQuery } from '@/features/agronomy/queries'
import { useInsightsQuery } from '@/features/ai/queries'
import { useFieldsQuery } from '@/features/fields/queries'
import { useIrrigationRecommendationsQuery } from '@/features/irrigation/queries'

import { toDecisionItems, type DecisionItem } from './model'

/** Composes the existing feature queries into one unified decision list. Keys are shared with the pages. */
export function useDecisionSources(farmId: string | null) {
  const prescriptions = usePrescriptionsQuery(farmId)
  const irrigation = useIrrigationRecommendationsQuery(farmId)
  const agronomy = useRecommendationsQuery(farmId)
  const insights = useInsightsQuery(farmId)
  const fields = useFieldsQuery(farmId)

  const items: DecisionItem[] = useMemo(
    () =>
      toDecisionItems({
        prescriptions: prescriptions.data,
        irrigation: irrigation.data,
        agronomy: agronomy.data,
        insights: insights.data,
        fields: fields.data,
      }),
    [prescriptions.data, irrigation.data, agronomy.data, insights.data, fields.data],
  )

  const queries = [prescriptions, irrigation, agronomy, insights, fields]
  const isLoading = queries.some((q) => q.isLoading)
  const isError = queries.every((q) => q.isError)
  const partialError = queries.some((q) => q.isError) && !isError
  const error = queries.find((q) => q.error)?.error ?? null

  return {
    items,
    isLoading,
    isError,
    partialError,
    error,
    refetch: () => queries.forEach((q) => void q.refetch()),
    fields: fields.data ?? [],
  }
}
