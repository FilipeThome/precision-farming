import { useEffect, useState } from 'react'

import { useInventoryCommands } from '@/features/inventory/queries'
import { inventoryUnitOptions } from '@/features/inventory/units'
import type { InventoryItem } from '@/shared/api/types'
import { useI18n } from '@/shared/i18n/useI18n'
import { queryError } from '@/shared/lib/queryError'
import { EntityFormDialog } from '@/shared/ui/EntityFormDialog'
import { useCodeOptions, useFarmScope } from '@/shared/ui/useFarmScope'

type Props = { open: boolean; item: InventoryItem | null; onClose: () => void }

export function InventoryFormDialog({ open, item, onClose }: Props) {
  const { t } = useI18n()
  const { farmField, defaultFarmId } = useFarmScope()
  const categories = useCodeOptions(['PESTICIDE', 'FERTILIZER', 'SEED', 'FUEL', 'PART'])
  const commands = useInventoryCommands()
  const [values, setValues] = useState(blank(defaultFarmId))

  useEffect(() => {
    if (!open) return
    setValues(
      item
        ? {
            farmId: item.farmId,
            name: item.name,
            category: item.category,
            unit: item.unit,
            quantity: String(item.quantity),
          }
        : blank(defaultFarmId),
    )
  }, [open, item, defaultFarmId])

  const failure = commands.create.error || commands.patch.error
  const fields = [
    farmField,
    { name: 'name', label: t('form.field.name'), type: 'text' as const, required: true },
    { name: 'category', label: t('form.field.category'), type: 'select' as const, required: true, options: categories },
    {
      name: 'unit',
      label: t('form.field.unit'),
      type: 'select' as const,
      required: true,
      options: inventoryUnitOptions(item?.unit),
    },
    ...(item
      ? []
      : [{ name: 'quantity', label: t('form.field.quantity'), type: 'number' as const, required: true, step: '0.1', min: '0' }]),
  ]

  return (
    <EntityFormDialog
      open={open}
      title={item ? t('form.edit') : t('form.new')}
      fields={fields}
      values={values}
      onChange={(name, value) => setValues((prev) => ({ ...prev, [name]: value }))}
      pending={commands.create.isPending || commands.patch.isPending}
      error={failure ? queryError(failure).message : null}
      onClose={onClose}
      onSubmit={() => {
        const base = {
          farmId: values.farmId,
          name: values.name.trim(),
          category: values.category,
          unit: values.unit.trim(),
        }
        const req = item
          ? commands.patch.mutateAsync({ id: item.id, body: base })
          : commands.create.mutateAsync({ ...base, quantity: Number(values.quantity) })
        void req.then(onClose)
      }}
    />
  )
}

function blank(farmId: string) {
  return { farmId, name: '', category: 'FERTILIZER', unit: 'KG', quantity: '' }
}
