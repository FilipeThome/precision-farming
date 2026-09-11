import { ChevronDown, LandPlot, LogOut } from 'lucide-react'
import { useNavigate } from 'react-router'

import { queryClient } from '@/app/queryClient'
import { logoutRequest } from '@/features/auth/api'
import { useFarmsQuery } from '@/features/farms/queries'
import { useAuthStore } from '@/shared/auth/store'
import { LocaleToggle } from '@/shared/i18n/LocaleToggle'
import { useI18n } from '@/shared/i18n/useI18n'
import { useFarmFromSearch } from '@/shared/lib/useFarmFromSearch'
import { useSearchParam } from '@/shared/lib/useSearchParam'
import { useFormat } from '@/shared/lib/useFormat'
import { Button } from '@/shared/ui/Button'
import { useUiStore } from '@/shared/ui/uiStore'

import { TrustStrip } from './TrustStrip'

export function Topbar() {
  useFarmFromSearch()
  const navigate = useNavigate()
  const name = useAuthStore((s) => s.name)
  const clearSession = useAuthStore((s) => s.clearSession)
  const farmId = useUiStore((s) => s.farmId)
  const setFarmId = useUiStore((s) => s.setFarmId)
  const [, setFarmParam] = useSearchParam('farm')
  const farms = useFarmsQuery()
  const { t } = useI18n()
  const { label } = useFormat()

  async function onLogout() {
    const refreshToken = useAuthStore.getState().refreshToken
    try {
      await logoutRequest(refreshToken)
    } catch {
      // local sign-out still proceeds
    }
    clearSession()
    queryClient.clear()
    navigate('/login', { replace: true })
  }

  return (
    <header className="flex h-14 shrink-0 items-center gap-3 border-b border-ag-n-200 bg-ag-n-0 px-5">
      <label className="relative inline-flex items-center gap-1.5 rounded-[8px] border border-ag-n-200 bg-ag-n-50 px-2.5 py-1.5 text-[13px] font-semibold text-ag-n-900 focus-within:border-ag-t-500">
        <LandPlot className="h-4 w-4 text-ag-g-700" aria-hidden />
        <span className="sr-only">{t('chrome.farm')}</span>
        <select
          className="appearance-none bg-transparent pr-5 font-semibold outline-none"
          value={farmId ?? ''}
          onChange={(e) => {
            const next = e.target.value || null
            setFarmId(next)
            setFarmParam(next)
          }}
        >
          <option value="">{t('chrome.allFarms')}</option>
          {(farms.data ?? []).map((farm) => (
            <option key={farm.id} value={farm.id}>
              {label(farm.id, farm.name)}
            </option>
          ))}
        </select>
        <ChevronDown className="pointer-events-none absolute right-2 h-4 w-4 text-ag-n-500" aria-hidden />
      </label>
      <div className="ml-auto flex items-center gap-3">
        <TrustStrip farmId={farmId} />
        <LocaleToggle />
        <span className="hidden text-[13px] text-ag-n-600 sm:inline">{name}</span>
        <Button variant="secondary" size="sm" onClick={onLogout}>
          <LogOut className="h-4 w-4" aria-hidden />
          {t('chrome.logout')}
        </Button>
      </div>
    </header>
  )
}
