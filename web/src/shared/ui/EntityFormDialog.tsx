import type { FormEvent, ReactNode } from 'react'
import { useEffect, useRef } from 'react'
import { createPortal } from 'react-dom'

import { useI18n } from '@/shared/i18n/useI18n'
import { Button } from '@/shared/ui/Button'

export type EntityFormField = {
  name: string
  label: string
  type: 'text' | 'number' | 'select' | 'date' | 'textarea'
  required?: boolean
  options?: Array<{ value: string; label: string }>
  step?: string
  min?: string
}

type Props = {
  open: boolean
  title: string
  fields: EntityFormField[]
  values: Record<string, string>
  onChange: (name: string, value: string) => void
  onSubmit: () => void
  onClose: () => void
  pending?: boolean
  error?: string | null
  children?: ReactNode
}

const FOCUSABLE = 'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
const INPUT =
  'min-h-11 rounded-[8px] border border-ag-n-300 px-3 py-2 text-[13px] text-ag-n-900'

export function EntityFormDialog({
  open,
  title,
  fields,
  values,
  onChange,
  onSubmit,
  onClose,
  pending,
  error,
  children,
}: Props) {
  const { t } = useI18n()
  const panelRef = useRef<HTMLDivElement>(null)
  const firstRef = useRef<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement | null>(null)

  useEffect(() => {
    if (!open) return
    const prev = document.activeElement as HTMLElement | null
    firstRef.current?.focus()
    function onKey(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        event.preventDefault()
        onClose()
        return
      }
      if (event.key !== 'Tab' || !panelRef.current) return
      const nodes = [...panelRef.current.querySelectorAll<HTMLElement>(FOCUSABLE)]
      if (nodes.length === 0) return
      const first = nodes[0]
      const last = nodes[nodes.length - 1]
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault()
        last.focus()
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault()
        first.focus()
      }
    }
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('keydown', onKey)
      prev?.focus?.()
    }
  }, [open, onClose])

  if (!open) return null

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    onSubmit()
  }

  return createPortal(
    <div className="fixed inset-0 z-[60] flex items-start justify-center overflow-y-auto bg-black/40 p-4 md:items-center">
      <div
        ref={panelRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="entity-form-title"
        aria-busy={pending || undefined}
        className="relative w-full max-w-lg rounded-[12px] bg-white p-5 shadow-xl"
      >
        <h2 id="entity-form-title" className="font-display text-lg font-bold text-ag-n-900">
          {title}
        </h2>
        <form className="mt-4 flex flex-col gap-3" onSubmit={handleSubmit}>
          {fields.map((field, index) => (
            <label key={field.name} htmlFor={`ef-${field.name}`} className="flex flex-col gap-1 text-[13px] text-ag-n-700">
              {field.label}
              {field.type === 'select' ? (
                <select
                  id={`ef-${field.name}`}
                  ref={index === 0 ? (el) => { firstRef.current = el } : undefined}
                  required={field.required}
                  value={values[field.name] ?? ''}
                  onChange={(e) => onChange(field.name, e.target.value)}
                  className={INPUT}
                >
                  {(field.options ?? []).map((opt) => (
                    <option key={opt.value} value={opt.value}>
                      {opt.label}
                    </option>
                  ))}
                </select>
              ) : field.type === 'textarea' ? (
                <textarea
                  id={`ef-${field.name}`}
                  ref={index === 0 ? (el) => { firstRef.current = el } : undefined}
                  required={field.required}
                  value={values[field.name] ?? ''}
                  onChange={(e) => onChange(field.name, e.target.value)}
                  className={`${INPUT} min-h-24`}
                />
              ) : (
                <input
                  id={`ef-${field.name}`}
                  ref={index === 0 ? (el) => { firstRef.current = el } : undefined}
                  type={field.type}
                  required={field.required}
                  step={field.step}
                  min={field.min}
                  value={values[field.name] ?? ''}
                  onChange={(e) => onChange(field.name, e.target.value)}
                  className={INPUT}
                />
              )}
            </label>
          ))}
          {children}
          {error ? (
            <p className="text-sm text-red-800" role="alert">
              {error}
            </p>
          ) : null}
          <div className="mt-1 flex justify-end gap-2">
            <Button type="button" variant="secondary" onClick={onClose}>
              {t('form.cancel')}
            </Button>
            <Button type="submit" disabled={pending}>
              {pending ? t('form.saving') : t('form.save')}
            </Button>
          </div>
        </form>
      </div>
    </div>,
    document.body,
  )
}
