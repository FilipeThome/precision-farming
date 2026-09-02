export const LOCALES = ['pt-BR', 'en-US'] as const

export type Locale = (typeof LOCALES)[number]

export const DEFAULT_LOCALE: Locale = 'pt-BR'

export function isLocale(value: unknown): value is Locale {
  return value === 'pt-BR' || value === 'en-US'
}
