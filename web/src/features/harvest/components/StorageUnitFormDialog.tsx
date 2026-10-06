import { useEffect, useState } from 'react'

import { useStorageUnitCommands } from '@/features/harvest/queries'
import type { StorageUnit } from '@/features/harvest/types'
import { useI18n } from '@/shared/i18n/useI18n'
import { queryError } from '@/shared/lib/queryError'
import { EntityFormDialog } from '@/shared/ui/EntityFormDialog'
import { useCodeOptions, useFarmScope } from '@/shared/ui/useFarmScope'

type Props = { open: boolean; unit: StorageUnit | null; onClose: () => void }

export function StorageUnitFormDialog({ open, unit, onClose }: Props) {
  const { t } = useI18n()
  const { farmField, defaultFarmId } = useFarmScope()
  const types = useCodeOptions(['SILO', 'WAREHOUSE'])
  const commands = useStorageUnitCommands()
  const [values, setValues] = useState(blank(defaultFarmId))

  useEffect(() => {
    if (!open) return
    setValues(
      unit
        ? {
            farmId: unit.farmId ?? defaultFarmId,
            name: unit.name ?? '',
            type: unit.type ?? 'SILO',
            capacityT: String(unit.capacityT ?? ''),
          }
        : blank(defaultFarmId),
    )
  }, [open, unit, defaultFarmId])

  const failure = commands.create.error || commands.patch.error
  return (
    <EntityFormDialog
      open={open}
      title={unit ? t('form.edit') : t('form.new')}
      fields={[
        farmField,
        { name: 'name', label: t('form.field.name'), type: 'text', required: true },
        { name: 'type', label: t('form.field.type'), type: 'select', required: true, options: types },
        { name: 'capacityT', label: t('form.field.capacityT'), type: 'number', required: true, step: '0.1', min: '0.1' },
      ]}
      values={values}
      onChange={(name, value) => setValues((prev) => ({ ...prev, [name]: value }))}
      pending={commands.create.isPending || commands.patch.isPending}
      error={failure ? queryError(failure).message : null}
      onClose={onClose}
      onSubmit={() => {
        const body = {
          farmId: values.farmId,
          name: values.name.trim(),
          type: values.type,
          capacityT: Number(values.capacityT),
          usedT: 0,
        }
        const req = unit
          ? commands.patch.mutateAsync({ id: unit.id, body })
          : commands.create.mutateAsync(body)
        void req.then(onClose)
      }}
    />
  )
}

function blank(farmId: string) {
  return { farmId, name: '', type: 'SILO', capacityT: '' }
}
