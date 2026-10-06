import { QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router'

import { ErrorBoundary } from '@/shared/ui/ErrorBoundary'

import { queryClient } from './queryClient'
import { AppRouter } from './router'

export function AppProviders() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <ErrorBoundary remountOnPathname={false}>
          <AppRouter />
        </ErrorBoundary>
      </BrowserRouter>
    </QueryClientProvider>
  )
}
