import type { ReactNode } from 'react'

import { useI18n } from '@/shared/i18n/useI18n'
import { queryError } from '@/shared/lib/queryError'

type Props = {
  isLoading: boolean
  error: unknown
  onRetry?: () => void
  children: ReactNode
}

export function InspectorQueryState({ isLoading, error, onRetry, children }: Props) {
  const { t } = useI18n()
  const err = queryError(error)
  if (isLoading) {
    return (
      <p aria-busy="true" className="text-sm text-pf-muted">
        {t('common.loading')}
      </p>
    )
  }
  if (error) {
    return (
      <div role="alert" className="flex flex-col gap-2 text-sm text-red-800">
        <p>{err.message || t('common.loadError')}</p>
        {onRetry ? (
          <button type="button" className="self-start underline" onClick={onRetry}>
            {t('common.retry')}
          </button>
        ) : null}
      </div>
    )
  }
  return <>{children}</>
}
