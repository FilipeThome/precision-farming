import { useEffect, useState } from 'react'

import { useWorkOrderCommands } from '@/features/maintenance/queries'
import { useMachinesQuery } from '@/features/machines/queries'
import { useI18n } from '@/shared/i18n/useI18n'
import { findInFarmScope } from '@/shared/lib/inFarmScope'
import { queryError } from '@/shared/lib/queryError'
import { EntityFormDialog } from '@/shared/ui/EntityFormDialog'
import { useCodeOptions, useFarmScope } from '@/shared/ui/useFarmScope'

type Props = { open: boolean; onClose: () => void }

export function WorkOrderFormDialog({ open, onClose }: Props) {
  const { t } = useI18n()
  const { farmField, defaultFarmId } = useFarmScope()
  const priorities = useCodeOptions(['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'])
  const create = useWorkOrderCommands()
  const [values, setValues] = useState(blank(defaultFarmId))
  const machines = useMachinesQuery(values.farmId || null)

  useEffect(() => {
    if (!open) return
    setValues(blank(defaultFarmId))
  }, [open, defaultFarmId])

  useEffect(() => {
    if (!open) return
    const list = machines.data
    if (list == null) return
    setValues((prev) => {
      if (list.length === 0) return prev.machineId ? { ...prev, machineId: '' } : prev
      if (list.some((row) => row.id === prev.machineId)) return prev
      return { ...prev, machineId: list[0].id }
    })
  }, [open, machines.data])

  const scoped = findInFarmScope(machines.data, values.machineId, values.farmId)
  const scopeError = machines.isSuccess && !scoped ? t('form.validation.machineNotInFarm') : null
  const submitDisabled = !machines.isSuccess || !scoped

  return (
    <EntityFormDialog
      open={open}
      title={t('form.new')}
      fields={[
        farmField,
        {
          name: 'machineId',
          label: t('form.field.machine'),
          type: 'select',
          required: true,
          options: (machines.data ?? []).map((row) => ({ value: row.id, label: row.name })),
        },
        { name: 'title', label: t('form.field.title'), type: 'text', required: true },
        { name: 'priority', label: t('form.field.priority'), type: 'select', required: true, options: priorities },
      ]}
      values={values}
      onChange={(name, value) =>
        setValues((prev) => ({ ...prev, [name]: value, ...(name === 'farmId' ? { machineId: '' } : {}) }))
      }
      pending={create.isPending}
      submitDisabled={submitDisabled}
      error={scopeError ?? (create.error ? queryError(create.error).message : null)}
      onClose={onClose}
      onSubmit={() => {
        if (submitDisabled || !scoped) return
        void create
          .mutateAsync({
            farmId: scoped.farmId ?? values.farmId,
            machineId: values.machineId,
            title: values.title.trim(),
            priority: values.priority,
          })
          .then(onClose)
      }}
    />
  )
}

function blank(farmId: string, machineId = '') {
  return { farmId, machineId, title: '', priority: 'MEDIUM' }
}
