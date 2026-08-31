import type { LucideIcon } from 'lucide-react'
import {
  Bell,
  Brain,
  CloudSun,
  FileSpreadsheet,
  LandPlot,
  LayoutDashboard,
  ListChecks,
  Map,
  Package,
  Settings,
  Square,
  Tractor,
  Wrench,
} from 'lucide-react'
import { NavLink } from 'react-router'

import { useUiStore } from '@/shared/ui/uiStore'

export const NAV_ITEMS: { to: string; label: string; icon: LucideIcon }[] = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/map', label: 'Mapa', icon: Map },
  { to: '/farms', label: 'Fazendas', icon: LandPlot },
  { to: '/fields', label: 'Talhões', icon: Square },
  { to: '/machines', label: 'Máquinas', icon: Tractor },
  { to: '/operations', label: 'Operações', icon: ListChecks },
  { to: '/maintenance', label: 'Manutenção', icon: Wrench },
  { to: '/inventory', label: 'Estoque', icon: Package },
  { to: '/weather', label: 'Clima', icon: CloudSun },
  { to: '/ai', label: 'IA & Insights', icon: Brain },
  { to: '/alerts', label: 'Alertas', icon: Bell },
  { to: '/reports', label: 'Relatórios', icon: FileSpreadsheet },
  { to: '/settings', label: 'Configurações', icon: Settings },
]

export function Sidebar() {
  const collapsed = useUiStore((s) => s.sidebarCollapsed)

  return (
    <aside
      className={`flex h-full flex-col bg-pf-green text-white transition-all duration-200 ${collapsed ? 'w-[72px]' : 'w-60'}`}
    >
      <div className="flex h-16 items-center gap-2 px-4 text-sm font-semibold tracking-wide">
        <span className="inline-flex h-8 w-8 items-center justify-center rounded-[12px] bg-white/15">
          PF
        </span>
        {!collapsed ? <span>Precision Farming</span> : null}
      </div>
      <nav className="flex-1 overflow-y-auto px-2 pb-4" aria-label="Principal">
        {NAV_ITEMS.map((item) => {
          const Icon = item.icon
          return (
            <NavLink
              key={item.to}
              to={item.to}
              title={item.label}
              className={({ isActive }) =>
                `mb-1 flex items-center gap-3 rounded-[12px] px-3 py-2 text-sm transition duration-150 ${
                  isActive ? 'bg-white/15 text-white' : 'text-white/80 hover:bg-white/10 hover:text-white'
                }`
              }
            >
              <Icon className="h-5 w-5 shrink-0" aria-hidden />
              {!collapsed ? item.label : <span className="sr-only">{item.label}</span>}
            </NavLink>
          )
        })}
      </nav>
    </aside>
  )
}
