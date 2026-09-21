import { useEffect, useState } from 'react'

import { useI18n } from '@/shared/i18n/useI18n'

const ACCEPT = 'image/jpeg,image/png,image/webp'
const MAX_BYTES = 5 * 1024 * 1024

type Props = {
  file: File | null
  onChange: (file: File | null) => void
  error?: string | null
  onInvalid?: (message: string | null) => void
}

export function PhotoField({ file, onChange, error, onInvalid }: Props) {
  const { t } = useI18n()
  const [preview, setPreview] = useState<string | null>(null)
  const [localError, setLocalError] = useState<string | null>(null)

  useEffect(() => {
    if (!file) {
      setPreview(null)
      return
    }
    const url = URL.createObjectURL(file)
    setPreview(url)
    return () => URL.revokeObjectURL(url)
  }, [file])

  const shownError = error ?? localError

  return (
    <div className="flex flex-col gap-1 text-[13px] text-ag-n-700">
      <label htmlFor="machine-photo">{t('form.photo')}</label>
      <input
        id="machine-photo"
        type="file"
        accept={ACCEPT}
        onChange={(event) => {
          const next = event.target.files?.[0] ?? null
          event.target.value = ''
          if (!next) {
            setLocalError(null)
            onInvalid?.(null)
            onChange(null)
            return
          }
          const okType = ACCEPT.split(',').includes(next.type) || /\.(jpe?g|png|webp)$/i.test(next.name)
          const okSize = next.size > 0 && next.size <= MAX_BYTES
          if (!okType || !okSize) {
            const message = t('form.photoInvalid')
            setLocalError(message)
            onInvalid?.(message)
            onChange(null)
            return
          }
          setLocalError(null)
          onInvalid?.(null)
          onChange(next)
        }}
        className="min-h-11 text-[13px]"
      />
      {preview ? (
        <img src={preview} alt={t('form.photoPreview')} className="mt-1 h-24 w-24 rounded-[10px] object-cover" />
      ) : null}
      {shownError ? (
        <p className="text-sm text-red-800" role="alert">
          {shownError}
        </p>
      ) : null}
    </div>
  )
}
