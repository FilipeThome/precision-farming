import { create } from 'zustand'
import { persist } from 'zustand/middleware'

type UiState = {
  farmId: string | null
  sidebarCollapsed: boolean
  setFarmId: (farmId: string | null) => void
  toggleSidebar: () => void
}

export const useUiStore = create<UiState>()(
  persist(
    (set) => ({
      farmId: null,
      sidebarCollapsed: false,
      setFarmId: (farmId) => set({ farmId }),
      toggleSidebar: () => set((s) => ({ sidebarCollapsed: !s.sidebarCollapsed })),
    }),
    {
      name: 'pf-ui',
      partialize: (s) => ({ farmId: s.farmId, sidebarCollapsed: s.sidebarCollapsed }),
    },
  ),
)
