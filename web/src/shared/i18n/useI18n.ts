import { enUS } from './dictionaries/en-US'
import { ptBR, type MessageKey } from './dictionaries/pt-BR'
import { DEFAULT_LOCALE, type Locale } from './locales'
import { useUiStore } from '@/shared/ui/uiStore'

const dictionaries: Record<Locale, Record<MessageKey, string>> = {
  'pt-BR': ptBR,
  'en-US': enUS,
}

export type TranslateVars = Record<string, string | number>

export function translate(locale: Locale, key: MessageKey, vars?: TranslateVars): string {
  const template = dictionaries[locale][key] ?? dictionaries[DEFAULT_LOCALE][key] ?? key
  if (!vars) return template
  return Object.entries(vars).reduce(
    (text, [name, value]) => text.replaceAll(`{${name}}`, String(value)),
    template,
  )
}

export function useI18n() {
  const locale = useUiStore((s) => s.locale)
  const setLocale = useUiStore((s) => s.setLocale)

  function t(key: MessageKey, vars?: TranslateVars): string {
    return translate(locale, key, vars)
  }

  return { locale, setLocale, t }
}

export type { MessageKey }
