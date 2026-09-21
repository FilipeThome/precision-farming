import { useEffect, useState } from 'react'

import { useFieldsQuery } from '@/features/fields/queries'
import { useIrrigationAssetCommands } from '@/features/irrigation/queries'
import type { IrrigationAsset } from '@/shared/api/types'
import { useI18n } from '@/shared/i18n/useI18n'
import { queryError } from '@/shared/lib/queryError'
import { EntityFormDialog } from '@/shared/ui/EntityFormDialog'
import { useCodeOptions, useFarmScope } from '@/shared/ui/useFarmScope'

type Props = { open: boolean; asset: IrrigationAsset | null; onClose: () => void }

export function IrrigationAssetFormDialog({ open, asset, onClose }: Props) {
  const { t } = useI18n()
  const { farmField, defaultFarmId } = useFarmScope()
  const types = useCodeOptions(['PIVOT', 'DRIP', 'SPRINKLER', 'PUMP', 'RESERVOIR'])
  const statuses = useCodeOptions(['IDLE', 'RUNNING', 'MAINTENANCE'])
  const commands = useIrrigationAssetCommands()
  const [values, setValues] = useState(blank(defaultFarmId))
  const fields = useFieldsQuery(values.farmId || null)

  useEffect(() => {
    if (!open) return
    setValues(
      asset
        ? {
            farmId: asset.farmId ?? defaultFarmId,
            fieldId: asset.fieldId ?? '',
            name: asset.name ?? '',
            type: asset.type ?? 'PIVOT',
            status: asset.status ?? 'IDLE',
            capacityMmH: asset.capacityMmH != null ? String(asset.capacityMmH) : '',
          }
        : blank(defaultFarmId),
    )
  }, [open, asset, defaultFarmId])

  useEffect(() => {
    if (!open || !values.fieldId) return
    const list = fields.data
    if (list == null) return
    if (list.length === 0) return
    if (!list.some((row) => row.id === values.fieldId)) {
      setValues((prev) => ({ ...prev, fieldId: '' }))
    }
  }, [open, fields.data, values.fieldId])

  const failure = commands.create.error || commands.patch.error
  return (
    <EntityFormDialog
      open={open}
      title={asset ? t('form.edit') : t('form.new')}
      fields={[
        farmField,
        {
          name: 'fieldId',
          label: t('form.field.field'),
          type: 'select',
          options: [
            { value: '', label: t('form.none') },
            ...(fields.data ?? []).map((row) => ({ value: row.id, label: row.name })),
          ],
        },
        { name: 'name', label: t('form.field.name'), type: 'text', required: true },
        { name: 'type', label: t('form.field.type'), type: 'select', required: true, options: types },
        { name: 'status', label: t('form.field.status'), type: 'select', required: true, options: statuses },
        { name: 'capacityMmH', label: t('form.field.capacityMmH'), type: 'number', step: '0.1', min: '0' },
      ]}
      values={values}
      onChange={(name, value) =>
        setValues((prev) => ({ ...prev, [name]: value, ...(name === 'farmId' ? { fieldId: '' } : {}) }))
      }
      pending={commands.create.isPending || commands.patch.isPending}
      error={failure ? queryError(failure).message : null}
      onClose={onClose}
      onSubmit={() => {
        const field = (fields.data ?? []).find((row) => row.id === values.fieldId)
        if (values.fieldId && (!field || field.farmId !== values.farmId)) return
        const body = {
          farmId: values.farmId,
          fieldId: values.fieldId || null,
          name: values.name.trim(),
          type: values.type,
          status: values.status,
          capacityMmH: values.capacityMmH === '' ? null : Number(values.capacityMmH),
        }
        const req = asset
          ? commands.patch.mutateAsync({ id: asset.id, body })
          : commands.create.mutateAsync(body)
        void req.then(onClose)
      }}
    />
  )
}

function blank(farmId: string) {
  return { farmId, fieldId: '', name: '', type: 'PIVOT', status: 'IDLE', capacityMmH: '' }
}
