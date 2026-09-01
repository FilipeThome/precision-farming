import { Outlet } from 'react-router'

import { useI18n } from '@/shared/i18n/useI18n'
import { useOnline } from '@/shared/lib/useOnline'
import { ErrorBoundary } from '@/shared/ui/ErrorBoundary'

import { Header } from './Header'
import { Sidebar } from './Sidebar'

export function AppShell() {
  const online = useOnline()
  const { t } = useI18n()

  return (
    <div className="flex h-screen overflow-hidden bg-pf-bg">
      <Sidebar />
      <div className="flex min-w-0 flex-1 flex-col">
        <Header />
        {!online ? (
          <div className="bg-amber-100 px-4 py-2 text-sm text-amber-950" role="status">
            {t('chrome.offlineBanner')}
          </div>
        ) : null}
        <main className="min-h-0 flex-1 overflow-y-auto p-6">
          <ErrorBoundary>
            <Outlet />
          </ErrorBoundary>
        </main>
      </div>
    </div>
  )
}
