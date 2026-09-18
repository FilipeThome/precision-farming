import { useI18n } from '@/shared/i18n/useI18n'

type Variant = 'wordmark' | 'mark'

const SRC: Record<Variant, string> = {
  wordmark: '/brand/precision-wordmark.png',
  mark: '/brand/precision-mark.svg',
}

type BrandLogoProps = {
  variant?: Variant
  className?: string
  /** Empty alt when a visible heading already names the brand. */
  decorative?: boolean
}

export function BrandLogo({ variant = 'mark', className = '', decorative = false }: BrandLogoProps) {
  const { t } = useI18n()
  return (
    <img
      src={SRC[variant]}
      alt={decorative ? '' : t('chrome.brand')}
      className={className}
      draggable={false}
    />
  )
}
