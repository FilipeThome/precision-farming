import { domainLabel } from '@/shared/i18n/domainLabels'
import type { Locale } from '@/shared/i18n/locales'
import { DEFAULT_LOCALE } from '@/shared/i18n/locales'

export function formatStatus(value: string, locale: Locale = DEFAULT_LOCALE): string {
  return domainLabel(locale, value)
}

export function formatNumber(value: number, digits = 0, locale: Locale = DEFAULT_LOCALE): string {
  return new Intl.NumberFormat(locale, {
    maximumFractionDigits: digits,
    minimumFractionDigits: digits,
  }).format(value)
}

export function formatDateTime(value: string | null | undefined, locale: Locale = DEFAULT_LOCALE): string {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat(locale, {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(date)
}

export function formatDate(value: string | null | undefined, locale: Locale = DEFAULT_LOCALE): string {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat(locale, { dateStyle: 'medium' }).format(date)
}

export function formatPercent(value: number, locale: Locale = DEFAULT_LOCALE): string {
  return `${formatNumber(value * 100, 0, locale)}%`
}
