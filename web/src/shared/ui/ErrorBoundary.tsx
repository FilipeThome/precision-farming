import { Component, type ReactNode } from 'react'
import { AlertTriangle } from 'lucide-react'
import { useLocation } from 'react-router'

import { useI18n } from '@/shared/i18n/useI18n'

import { Button } from './Button'
import { Card } from './Card'

type Props = { children: ReactNode }
type State = { error: Error | null }

function ErrorFallback({ message, onRetry }: { message?: string; onRetry: () => void }) {
  const { t } = useI18n()

  return (
    <Card className="flex flex-col items-start gap-3">
      <div className="flex items-center gap-2 text-red-800">
        <AlertTriangle className="h-5 w-5" aria-hidden />
        <strong>{t('common.loadError')}</strong>
      </div>
      <p className="text-sm text-pf-muted">{message || t('common.unexpectedError')}</p>
      <Button onClick={onRetry} variant="secondary">
        {t('common.retry')}
      </Button>
    </Card>
  )
}

class RouteErrorBoundary extends Component<Props, State> {
  state: State = { error: null }

  static getDerivedStateFromError(error: Error): State {
    return { error }
  }

  render() {
    if (this.state.error) {
      return (
        <ErrorFallback
          message={this.state.error.message}
          onRetry={() => this.setState({ error: null })}
        />
      )
    }
    return this.props.children
  }
}

export function ErrorBoundary({ children }: Props) {
  const location = useLocation()
  return <RouteErrorBoundary key={location.pathname}>{children}</RouteErrorBoundary>
}
