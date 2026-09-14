import { Outlet } from 'react-router'

import { useIdleSessionTimeout } from '@/shared/auth/useIdleSessionTimeout'
import { useI18n } from '@/shared/i18n/useI18n'
import { useOnline } from '@/shared/lib/useOnline'
import { ErrorBoundary } from '@/shared/ui/ErrorBoundary'

import { Rail } from './Rail'
import { Topbar } from './Topbar'

export function AppShell() {
  const online = useOnline()
  const { t } = useI18n()
  useIdleSessionTimeout()

  return (
    <div className="flex h-screen overflow-hidden bg-ag-n-50">
      <Rail />
      <div className="flex min-w-0 flex-1 flex-col">
        <Topbar />
        {!online ? (
          <div className="bg-ag-n-800 px-5 py-1.5 text-xs font-medium text-[#e8e6df]" role="status">
            {t('chrome.offlineBanner')}
          </div>
        ) : null}
        <main className="flex min-h-0 flex-1 flex-col overflow-y-auto px-6 py-4">
          <ErrorBoundary>
            <Outlet />
          </ErrorBoundary>
        </main>
      </div>
    </div>
  )
}
