import type { CSSProperties } from 'react'

import { useI18n } from '@/shared/i18n/useI18n'

export type ConfidenceLevel = 'hi' | 'md' | 'lo'

export function confidenceLevel(value: number): ConfidenceLevel {
  if (value >= 0.75) return 'hi'
  if (value >= 0.5) return 'md'
  return 'lo'
}

const COLOR: Record<ConfidenceLevel, string> = {
  hi: 'var(--color-ag-conf-hi)',
  md: 'var(--color-ag-conf-md)',
  lo: 'var(--color-ag-conf-lo)',
}

type ConfidenceMeterProps = {
  /** 0..1. When undefined/null/NaN nothing is rendered — never a placeholder score. */
  value?: number | null
  /** Optional secondary line under the label. */
  hint?: string
  size?: 'sm' | 'lg'
  showLabel?: boolean
  className?: string
}

/** Conic ring + numeric 0.00–1.00 + level label (alta ≥0.75 / média ≥0.5 / baixa). */
export function ConfidenceMeter({ value, hint, size = 'sm', showLabel = true, className = '' }: ConfidenceMeterProps) {
  const { t } = useI18n()
  if (value == null || Number.isNaN(value)) return null
  const clamped = Math.min(1, Math.max(0, value))
  const level = confidenceLevel(clamped)
  const pct = Math.round(clamped * 100)
  const color = COLOR[level]
  const ringStyle: CSSProperties = {
    background: `conic-gradient(${color} ${pct}%, var(--color-ag-n-200) 0)`,
    color,
  }
  const labelKey = level === 'hi' ? 'confidence.high' : level === 'md' ? 'confidence.medium' : 'confidence.low'
  const big = size === 'lg'

  return (
    <span
      className={`inline-flex items-center gap-2 ${className}`}
      role="meter"
      aria-valuemin={0}
      aria-valuemax={1}
      aria-valuenow={Number(clamped.toFixed(2))}
      aria-label={`${t('confidence.label')} ${clamped.toFixed(2)}`}
      data-level={level}
    >
      <span
        className={`relative grid place-items-center rounded-full ${big ? 'h-[120px] w-[120px]' : 'h-[34px] w-[34px]'}`}
        style={ringStyle}
      >
        <span className={`absolute rounded-full bg-ag-n-0 ${big ? 'inset-[10px]' : 'inset-1'}`} aria-hidden />
        <span className={`relative z-[1] font-display font-extrabold tnum ${big ? 'text-[30px]' : 'text-[11px]'}`}>
          {clamped.toFixed(2)}
        </span>
      </span>
      {showLabel ? (
        <span className="flex flex-col leading-[1.15]">
          <b className="text-xs">{t(labelKey)}</b>
          {hint ? <small className="text-[10.5px] text-ag-n-600">{hint}</small> : null}
        </span>
      ) : null}
    </span>
  )
}
