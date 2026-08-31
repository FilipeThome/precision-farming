import type { ReactNode } from 'react'
import { AlertTriangle, Inbox } from 'lucide-react'

import { Button } from './Button'
import { Card } from './Card'

type QueryPageStateProps = {
  isLoading: boolean
  isError: boolean
  errorMessage?: string
  correlationId?: string
  isEmpty: boolean
  emptyTitle: string
  emptyDescription?: string
  onRetry?: () => void
  children: ReactNode
}

export function QueryPageState({
  isLoading,
  isError,
  errorMessage,
  correlationId,
  isEmpty,
  emptyTitle,
  emptyDescription,
  onRetry,
  children,
}: QueryPageStateProps) {
  if (isLoading) {
    return (
      <div className="grid gap-3" aria-busy="true" aria-live="polite">
        <div className="h-24 animate-pulse rounded-[12px] bg-white" />
        <div className="h-40 animate-pulse rounded-[12px] bg-white" />
      </div>
    )
  }

  if (isError) {
    return (
      <Card className="flex flex-col items-start gap-3">
        <div className="flex items-center gap-2 text-red-800">
          <AlertTriangle className="h-5 w-5" aria-hidden />
          <strong>Não foi possível carregar os dados</strong>
        </div>
        <p className="text-sm text-pf-muted">{errorMessage ?? 'Erro inesperado.'}</p>
        {correlationId ? (
          <p className="text-xs text-pf-muted">
            ID de correlação: <code>{correlationId}</code>
          </p>
        ) : null}
        {onRetry ? (
          <Button onClick={onRetry} variant="secondary">
            Tentar novamente
          </Button>
        ) : null}
      </Card>
    )
  }

  if (isEmpty) {
    return (
      <Card className="flex flex-col items-start gap-2">
        <div className="flex items-center gap-2 text-pf-green">
          <Inbox className="h-5 w-5" aria-hidden />
          <strong>{emptyTitle}</strong>
        </div>
        {emptyDescription ? <p className="text-sm text-pf-muted">{emptyDescription}</p> : null}
      </Card>
    )
  }

  return <>{children}</>
}
