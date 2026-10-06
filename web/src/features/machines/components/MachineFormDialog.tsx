import { useEffect, useState } from 'react'

import { useMachineCommands } from '@/features/machines/queries'
import type { Machine } from '@/features/machines/types'
import { useI18n } from '@/shared/i18n/useI18n'
import { queryError } from '@/shared/lib/queryError'
import { EntityFormDialog } from '@/shared/ui/EntityFormDialog'
import { PhotoField } from '@/shared/ui/PhotoField'
import { useCodeOptions, useFarmScope } from '@/shared/ui/useFarmScope'

type Props = { open: boolean; machine: Machine | null; onClose: () => void }

export function MachineFormDialog({ open, machine, onClose }: Props) {
  const { t } = useI18n()
  const { farmField, defaultFarmId } = useFarmScope()
  const types = useCodeOptions(['TRACTOR', 'SPRAYER', 'HARVESTER', 'PLANTER', 'DRONE'])
  const statuses = useCodeOptions(['OPERATING', 'IDLE', 'MAINTENANCE'])
  const save = useMachineCommands()
  const [values, setValues] = useState(blank(defaultFarmId))
  const [photo, setPhoto] = useState<File | null>(null)
  const [photoError, setPhotoError] = useState<string | null>(null)
  const [removePhoto, setRemovePhoto] = useState(false)

  useEffect(() => {
    if (!open) return
    setPhoto(null)
    setPhotoError(null)
    setRemovePhoto(false)
    setValues(
      machine
        ? {
            farmId: machine.farmId,
            name: machine.name,
            type: machine.type,
            manufacturer: machine.manufacturer,
            model: machine.model,
            status: machine.status,
          }
        : blank(defaultFarmId),
    )
  }, [open, machine, defaultFarmId])

  const farmMoved = Boolean(machine && values.farmId !== machine.farmId)
  const photoFileId = photo
    ? undefined
    : removePhoto || farmMoved
      ? null
      : (machine?.photoFileId ?? null)

  return (
    <EntityFormDialog
      open={open}
      title={machine ? t('form.edit') : t('form.new')}
      fields={[
        farmField,
        { name: 'name', label: t('form.field.name'), type: 'text', required: true },
        { name: 'type', label: t('form.field.type'), type: 'select', required: true, options: types },
        { name: 'manufacturer', label: t('form.field.manufacturer'), type: 'text', required: true },
        { name: 'model', label: t('form.field.model'), type: 'text', required: true },
        { name: 'status', label: t('form.field.status'), type: 'select', required: true, options: statuses },
      ]}
      values={values}
      onChange={(name, value) => setValues((prev) => ({ ...prev, [name]: value }))}
      pending={save.isPending}
      error={photoError || (save.error ? queryError(save.error).message : null)}
      onClose={onClose}
      onSubmit={() => {
        if (photoError) return
        void save
          .mutateAsync({
            id: machine?.id,
            photo,
            body: {
              farmId: values.farmId,
              name: values.name.trim(),
              type: values.type,
              manufacturer: values.manufacturer.trim(),
              model: values.model.trim(),
              status: values.status,
              photoFileId: photoFileId ?? null,
            },
          })
          .then(onClose)
      }}
    >
      <PhotoField file={photo} onChange={setPhoto} onInvalid={setPhotoError} />
      {machine?.photoFileId && !photo && !removePhoto ? (
        <button type="button" className="text-left text-[13px] text-ag-n-700 underline" onClick={() => setRemovePhoto(true)}>
          {t('form.photoRemove')}
        </button>
      ) : null}
    </EntityFormDialog>
  )
}

function blank(farmId: string) {
  return { farmId, name: '', type: 'TRACTOR', manufacturer: '', model: '', status: 'IDLE' }
}
