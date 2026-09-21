import { useEffect, useState } from 'react'

import { useFieldsQuery } from '@/features/fields/queries'
import { useHarvestPlanCommands } from '@/features/harvest/queries'
import { useI18n } from '@/shared/i18n/useI18n'
import { findInFarmScope } from '@/shared/lib/inFarmScope'
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

  const scoped = findInFarmScope(fields.data, values.fieldId, values.farmId)
  const range = plannedRange(values.plannedStart, values.plannedEnd)
  const scopeError = fields.isSuccess && !scoped ? t('form.validation.fieldNotInFarm') : null
  const windowError = range.ok
    ? null
    : t(range.error === 'order' ? 'form.validation.plannedOrder' : 'form.validation.plannedWindow')
  const submitDisabled = !fields.isSuccess || !scoped || !range.ok

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
        { name: 'plannedStart', label: t('form.field.startDate'), type: 'date' },
        { name: 'plannedEnd', label: t('form.field.endDate'), type: 'date' },
      ]}
      values={values}
      onChange={(name, value) =>
        setValues((prev) => ({ ...prev, [name]: value, ...(name === 'farmId' ? { fieldId: '' } : {}) }))
      }
      pending={create.isPending}
      submitDisabled={submitDisabled}
      error={scopeError ?? windowError ?? (create.error ? queryError(create.error).message : null)}
      onClose={onClose}
      onSubmit={() => {
        if (submitDisabled || !range.ok || !scoped) return
        void create
          .mutateAsync({
            farmId: values.farmId,
            fieldId: values.fieldId,
            crop: values.crop,
            expectedTHa: Number(values.expectedTHa),
            plannedStart: range.plannedStart,
            plannedEnd: range.plannedEnd,
          })
          .then(onClose)
      }}
    />
  )
}

function plannedRange(start: string, end: string) {
  if (!start && !end) return { ok: true as const, plannedStart: null, plannedEnd: null }
  if (!start || !end) return { ok: false as const, error: 'window' as const }
  if (start > end) return { ok: false as const, error: 'order' as const }
  return {
    ok: true as const,
    plannedStart: `${start}T00:00:00.000Z`,
    plannedEnd: `${end}T23:59:59.000Z`,
  }
}

function blank(farmId: string, fieldId = '') {
  return { farmId, fieldId, crop: 'SOY', expectedTHa: '', plannedStart: '', plannedEnd: '' }
}
