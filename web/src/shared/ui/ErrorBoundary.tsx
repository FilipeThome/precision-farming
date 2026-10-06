import { Component, type ReactNode } from 'react'
import { AlertTriangle } from 'lucide-react'
import { useLocation } from 'react-router'

import { useI18n } from '@/shared/i18n/useI18n'

import { Button } from './Button'
import { Card } from './Card'

type Props = {
  children: ReactNode
  /**
   * Outlet boundary remounts on pathname so a crashed page clears when the operator navigates.
   * The router boundary passes false so a healthy tree is not remounted on every navigation.
   */
  remountOnPathname?: boolean
}
type State = { error: Error | null }
type BoundaryProps = { children: ReactNode; resetKey?: string }

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

class RouteErrorBoundary extends Component<BoundaryProps, State> {
  state: State = { error: null }

  static getDerivedStateFromError(error: Error): State {
    return { error }
  }

  componentDidUpdate(prev: BoundaryProps) {
    if (this.props.resetKey !== undefined && prev.resetKey !== this.props.resetKey && this.state.error) {
      this.setState({ error: null })
    }
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

export function ErrorBoundary({ children, remountOnPathname = true }: Props) {
  const location = useLocation()
  if (remountOnPathname) {
    return <RouteErrorBoundary key={location.pathname}>{children}</RouteErrorBoundary>
  }
  return <RouteErrorBoundary resetKey={location.pathname}>{children}</RouteErrorBoundary>
}
