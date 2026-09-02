import { create } from 'zustand'
import { persist } from 'zustand/middleware'

import { DEFAULT_LOCALE, isLocale, type Locale } from '@/shared/i18n/locales'

type UiState = {
  farmId: string | null
  sidebarCollapsed: boolean
  locale: Locale
  setFarmId: (farmId: string | null) => void
  toggleSidebar: () => void
  setLocale: (locale: Locale) => void
}

export const useUiStore = create<UiState>()(
  persist(
    (set) => ({
      farmId: null,
      sidebarCollapsed: false,
      locale: DEFAULT_LOCALE,
      setFarmId: (farmId) => set({ farmId }),
      toggleSidebar: () => set((s) => ({ sidebarCollapsed: !s.sidebarCollapsed })),
      setLocale: (locale) => set({ locale }),
    }),
    {
      name: 'pf-ui',
      partialize: (s) => ({
        farmId: s.farmId,
        sidebarCollapsed: s.sidebarCollapsed,
        locale: s.locale,
      }),
      merge: (persisted, current) => {
        const p = (persisted ?? {}) as Partial<UiState>
        return {
          ...current,
          ...p,
          locale: isLocale(p.locale) ? p.locale : current.locale,
        }
      },
    },
  ),
)
