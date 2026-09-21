import { useEffect, useState } from 'react'

import { useFieldsQuery } from '@/features/fields/queries'
import { useHarvestPlanCommands } from '@/features/harvest/queries'
import { useI18n } from '@/shared/i18n/useI18n'
import { queryError } from '@/shared/lib/queryError'
import { EntityFormDialog } from '@/shared/ui/EntityFormDialog'
import { useCodeOptions, useFarmScope } from '@/shared/ui/useFarmScope'

type Props = { open: boolean; onClose: () => void }

export function HarvestPlanFormDialog({ open, onClose }: Props) {
  const { t } = useI18n()
  const { farmField, defaultFarmId } = useFarmScope()
  const crops = useCodeOptions(['SOY', 'CORN', 'COTTON'])
  const create = useHarvestPlanCommands()
  const [values, setValues] = useState(blank(defaultFarmId))
  const fields = useFieldsQuery(values.farmId || null)

  useEffect(() => {
    if (!open) return
    setValues(blank(defaultFarmId))
  }, [open, defaultFarmId])

  useEffect(() => {
    if (!open) return
    const list = fields.data
    if (list == null) return
    setValues((prev) => {
      if (list.length === 0) return prev.fieldId ? { ...prev, fieldId: '' } : prev
      if (list.some((row) => row.id === prev.fieldId)) return prev
      return { ...prev, fieldId: list[0].id }
    })
  }, [open, fields.data])

  return (
    <EntityFormDialog
      open={open}
      title={t('form.new')}
      fields={[
        farmField,
        {
          name: 'fieldId',
          label: t('form.field.field'),
          type: 'select',
          required: true,
          options: (fields.data ?? []).map((row) => ({ value: row.id, label: row.name })),
        },
        { name: 'crop', label: t('form.field.crop'), type: 'select', required: true, options: crops },
        { name: 'expectedTHa', label: t('form.field.expectedTHa'), type: 'number', required: true, step: '0.1', min: '0' },
      ]}
      values={values}
      onChange={(name, value) =>
        setValues((prev) => ({ ...prev, [name]: value, ...(name === 'farmId' ? { fieldId: '' } : {}) }))
      }
      pending={create.isPending}
      error={create.error ? queryError(create.error).message : null}
      onClose={onClose}
      onSubmit={() => {
        const field = (fields.data ?? []).find((row) => row.id === values.fieldId)
        if (!field || field.farmId !== values.farmId) return
        void create
          .mutateAsync({
            farmId: values.farmId,
            fieldId: values.fieldId,
            crop: values.crop,
            expectedTHa: Number(values.expectedTHa),
          })
          .then(onClose)
      }}
    />
  )
}

function blank(farmId: string, fieldId = '') {
  return { farmId, fieldId, crop: 'SOY', expectedTHa: '' }
}
