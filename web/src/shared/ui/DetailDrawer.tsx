import { type ReactNode, useEffect, useRef } from 'react'
import { createPortal } from 'react-dom'
import { X } from 'lucide-react'

import { useI18n } from '@/shared/i18n/useI18n'

type DetailDrawerProps = {
  open: boolean
  title: string
  subtitle?: string
  onClose: () => void
  children: ReactNode
}

const FOCUSABLE = 'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'

export function DetailDrawer({ open, title, subtitle, onClose, children }: DetailDrawerProps) {
  const { t } = useI18n()
  const closeRef = useRef<HTMLButtonElement>(null)
  const panelRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open) return
    const prev = document.activeElement as HTMLElement | null
    closeRef.current?.focus()

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

  return createPortal(
    <div className="fixed inset-0 z-50 flex justify-end">
      <button
        type="button"
        className="absolute inset-0 bg-black/40"
        aria-label={t('inspector.close')}
        onClick={onClose}
      />
      <div
        ref={panelRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="inspector-title"
        className="relative flex h-full w-full flex-col bg-white shadow-xl md:w-[28rem]"
      >
        <div className="flex items-start justify-between gap-3 border-b border-pf-border p-4">
          <div className="min-w-0">
            <h2 id="inspector-title" className="text-lg font-semibold text-pf-green">
              {title}
            </h2>
            {subtitle ? <p className="mt-1 text-sm text-pf-muted">{subtitle}</p> : null}
          </div>
          <button
            ref={closeRef}
            type="button"
            onClick={onClose}
            className="rounded-[12px] p-2 text-pf-green hover:bg-pf-green/10 focus-visible:outline focus-visible:ring-2 focus-visible:ring-pf-teal"
            aria-label={t('inspector.close')}
          >
            <X className="h-5 w-5" aria-hidden />
          </button>
        </div>
        <div className="min-h-0 flex-1 overflow-y-auto p-4">{children}</div>
      </div>
    </div>,
    document.body,
  )
}
