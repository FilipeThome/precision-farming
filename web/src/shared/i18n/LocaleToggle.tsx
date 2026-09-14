import { useI18n } from '@/shared/i18n/useI18n'
import { Button } from '@/shared/ui/Button'

export function LocaleToggle() {
  const { locale, setLocale, t } = useI18n()

  return (
    <div className="inline-flex items-center gap-1" role="group" aria-label={t('locale.ptBR')}>
      <Button
        variant="ghost"
        className={`min-h-11 min-w-11 px-2 text-lg leading-none ${locale === 'pt-BR' ? 'bg-white/60 ring-1 ring-pf-border' : ''}`}
        aria-pressed={locale === 'pt-BR'}
        aria-label={t('locale.ptBR')}
        onClick={() => setLocale('pt-BR')}
      >
        <span aria-hidden>🇧🇷</span>
      </Button>
      <Button
        variant="ghost"
        className={`min-h-11 min-w-11 px-2 text-lg leading-none ${locale === 'en-US' ? 'bg-white/60 ring-1 ring-pf-border' : ''}`}
        aria-pressed={locale === 'en-US'}
        aria-label={t('locale.enUS')}
        onClick={() => setLocale('en-US')}
      >
        <span aria-hidden>🇺🇸</span>
      </Button>
    </div>
  )
}
