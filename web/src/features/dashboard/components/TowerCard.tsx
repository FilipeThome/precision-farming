import type { ReactNode } from 'react'
import { AlertTriangle } from 'lucide-react'

import { useI18n } from '@/shared/i18n/useI18n'
import { Button } from '@/shared/ui/Button'
import { Card } from '@/shared/ui/Card'

type TowerCardProps = {
  title: string
  aside?: ReactNode
  isLoading?: boolean
  isError?: boolean
  errorMessage?: string
  isEmpty?: boolean
  emptyText?: string
  onRetry?: () => void
  className?: string
  bodyClassName?: string
  children: ReactNode
}

/** Card shell with its own loading / error / empty state so each Tower card fails independently. */
export function TowerCard({
  title,
  aside,
  isLoading = false,
  isError = false,
  errorMessage,
  isEmpty = false,
  emptyText,
  onRetry,
  className = '',
  bodyClassName = '',
  children,
}: TowerCardProps) {
  const { t } = useI18n()
  return (
    <Card className={`flex flex-col p-0 ${className}`}>
      <div className="flex items-center justify-between gap-2 px-3.5 pb-2 pt-3">
        <h2 className="font-display text-base font-bold text-ag-n-900">{title}</h2>
        {aside}
      </div>
      <div className={`px-3.5 pb-3.5 ${bodyClassName}`}>
        {isLoading ? (
          <div className="flex flex-col gap-2" aria-busy="true" aria-live="polite">
            <div className="h-10 animate-pulse rounded-[10px] bg-ag-n-100" />
            <div className="h-10 animate-pulse rounded-[10px] bg-ag-n-100" />
          </div>
        ) : isError ? (
          <div className="flex flex-col items-start gap-2 text-sm" role="alert">
            <span className="flex items-center gap-1.5 text-ag-crit">
              <AlertTriangle className="h-4 w-4" aria-hidden />
              {t('common.loadError')}
            </span>
            {errorMessage ? <span className="text-xs text-ag-n-600">{errorMessage}</span> : null}
            {onRetry ? (
              <Button size="sm" variant="secondary" onClick={onRetry}>
                {t('common.retry')}
              </Button>
            ) : null}
          </div>
        ) : isEmpty ? (
          <p className="text-sm text-ag-n-600" role="status">
            {emptyText ?? t('tower.empty')}
          </p>
        ) : (
          children
        )}
      </div>
    </Card>
  )
}
