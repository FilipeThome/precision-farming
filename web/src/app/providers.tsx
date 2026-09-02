import { QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router'

import { queryClient } from './queryClient'
import { AppRouter } from './router'

export function AppProviders() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AppRouter />
      </BrowserRouter>
    </QueryClientProvider>
  )
}
