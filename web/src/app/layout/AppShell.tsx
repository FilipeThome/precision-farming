import { Outlet } from 'react-router'

import { useOnline } from '@/shared/lib/useOnline'

import { Header } from './Header'
import { Sidebar } from './Sidebar'

export function AppShell() {
  const online = useOnline()

  return (
    <div className="flex h-screen overflow-hidden bg-pf-bg">
      <Sidebar />
      <div className="flex min-w-0 flex-1 flex-col">
        <Header />
        {!online ? (
          <div className="bg-amber-100 px-4 py-2 text-sm text-amber-950" role="status">
            Você está offline. Os dados podem estar desatualizados.
          </div>
        ) : null}
        <main className="min-h-0 flex-1 overflow-y-auto p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
