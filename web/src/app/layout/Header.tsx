import { Bell, LogOut, Menu } from 'lucide-react'

import { useAlertsQuery } from '@/features/alerts/queries'
import { useFarmsQuery } from '@/features/farms/queries'
import { LocaleToggle } from '@/shared/i18n/LocaleToggle'
import { useI18n } from '@/shared/i18n/useI18n'
import { useAuthStore } from '@/shared/auth/store'
import { useOnline } from '@/shared/lib/useOnline'
import { Button } from '@/shared/ui/Button'
import { useUiStore } from '@/shared/ui/uiStore'

export function Header() {
  const name = useAuthStore((s) => s.name)
  const clearSession = useAuthStore((s) => s.clearSession)
  const farmId = useUiStore((s) => s.farmId)
  const setFarmId = useUiStore((s) => s.setFarmId)
  const toggleSidebar = useUiStore((s) => s.toggleSidebar)
  const farms = useFarmsQuery()
  const alerts = useAlertsQuery(farmId)
  const online = useOnline()
  const { t } = useI18n()
  const openAlerts = (alerts.data ?? []).filter((a) => a.status === 'OPEN').length

  return (
    <header className="flex h-16 items-center gap-3 border-b border-pf-border bg-white px-4">
      <Button variant="ghost" onClick={toggleSidebar} aria-label={t('chrome.toggleMenu')}>
        <Menu className="h-5 w-5" />
      </Button>
      <label className="flex items-center gap-2 text-sm text-pf-muted">
        <span className="sr-only">{t('chrome.farm')}</span>
        <select
          className="rounded-[12px] border border-pf-border bg-white px-3 py-2 text-sm text-pf-green"
          value={farmId ?? ''}
          onChange={(e) => setFarmId(e.target.value || null)}
        >
          <option value="">{t('chrome.allFarms')}</option>
          {(farms.data ?? []).map((farm) => (
            <option key={farm.id} value={farm.id}>
              {farm.name}
            </option>
          ))}
        </select>
      </label>
      {!online ? (
        <span className="rounded-full bg-amber-100 px-2 py-1 text-xs text-amber-900">{t('chrome.offline')}</span>
      ) : null}
      <div className="ml-auto flex items-center gap-3">
        <LocaleToggle />
        <span
          className="relative inline-flex text-pf-green"
          title={t('chrome.openAlerts', { count: openAlerts })}
        >
          <Bell className="h-5 w-5" aria-hidden />
          {openAlerts > 0 ? (
            <span className="absolute -right-2 -top-2 rounded-full bg-red-700 px-1.5 text-[10px] text-white">
              {openAlerts}
            </span>
          ) : null}
        </span>
        <span className="hidden text-sm text-pf-muted sm:inline">{name}</span>
        <Button variant="secondary" onClick={clearSession}>
          <LogOut className="h-4 w-4" aria-hidden />
          {t('chrome.logout')}
        </Button>
      </div>
    </header>
  )
}
