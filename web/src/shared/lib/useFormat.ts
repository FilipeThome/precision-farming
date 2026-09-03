import { domainLabel, entityDisplayName, isUuid } from '@/shared/i18n/domainLabels'
import { useI18n } from '@/shared/i18n/useI18n'
import { formatDate, formatDateTime, formatNumber, formatPercent } from '@/shared/lib/format'

export function useFormat() {
  const { locale } = useI18n()

  function label(value?: string | null, fallback?: string | null): string {
    if (value && isUuid(value)) {
      const named = entityDisplayName(locale, value)
      if (named) return named
    }
    if (value && !isUuid(value)) {
      return domainLabel(locale, value)
    }
    if (fallback && isUuid(fallback)) {
      const named = entityDisplayName(locale, fallback)
      if (named) return named
    }
    return domainLabel(locale, fallback ?? value)
  }

  return {
    locale,
    label,
    number: (value: number, digits = 0) => formatNumber(value, digits, locale),
    dateTime: (value?: string | null) => formatDateTime(value, locale),
    date: (value?: string | null) => formatDate(value, locale),
    percent: (value: number) => formatPercent(value, locale),
  }
}
