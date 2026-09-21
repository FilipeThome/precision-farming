import { useFarmsQuery } from '@/features/farms/queries'
import type { EntityFormField } from '@/shared/ui/EntityFormDialog'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFormat } from '@/shared/lib/useFormat'
import { useUiStore } from '@/shared/ui/uiStore'

export function useFarmScope() {
  const farmId = useUiStore((s) => s.farmId)
  const farms = useFarmsQuery()
  const { t } = useI18n()
  const field: EntityFormField = {
    name: 'farmId',
    label: t('chrome.farm'),
    type: 'select',
    required: true,
    options: (farms.data ?? []).map((farm) => ({ value: farm.id, label: farm.name })),
  }
  return { farmId, farmField: field, defaultFarmId: farmId ?? farms.data?.[0]?.id ?? '' }
}

export function useCodeOptions(codes: string[]) {
  const { label } = useFormat()
  return codes.map((value) => ({ value, label: label(value) }))
}
