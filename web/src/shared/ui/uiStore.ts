import { create } from 'zustand'
import { persist } from 'zustand/middleware'

import { DEFAULT_LOCALE, isLocale, type Locale } from '@/shared/i18n/locales'

type UiState = {
  farmId: string | null
  locale: Locale
  setFarmId: (farmId: string | null) => void
  setLocale: (locale: Locale) => void
}

export const useUiStore = create<UiState>()(
  persist(
    (set) => ({
      farmId: null,
      locale: DEFAULT_LOCALE,
      setFarmId: (farmId) => set({ farmId }),
      setLocale: (locale) => set({ locale }),
    }),
    {
      name: 'pf-ui',
      partialize: (s) => ({
        farmId: s.farmId,
        locale: s.locale,
      }),
      merge: (persisted, current) => {
        // Tolerates stale keys from older builds (e.g. `sidebarCollapsed`).
        const p = (persisted ?? {}) as Partial<UiState> & Record<string, unknown>
        return {
          ...current,
          farmId: typeof p.farmId === 'string' ? p.farmId : current.farmId,
          locale: isLocale(p.locale) ? p.locale : current.locale,
        }
      },
    },
  ),
)
