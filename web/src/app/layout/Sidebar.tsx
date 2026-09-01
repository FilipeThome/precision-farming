import type { LucideIcon } from 'lucide-react'
import {
  Bell,
  Brain,
  Cable,
  CloudSun,
  Droplets,
  FileSpreadsheet,
  Landmark,
  LandPlot,
  LayoutDashboard,
  ListChecks,
  Map,
  Package,
  Scale,
  Settings,
  ShieldCheck,
  Sprout,
  Square,
  Tractor,
  Wheat,
  Wrench,
} from 'lucide-react'
import { NavLink } from 'react-router'

import { useI18n } from '@/shared/i18n/useI18n'
import type { MessageKey } from '@/shared/i18n/useI18n'
import { useUiStore } from '@/shared/ui/uiStore'

type NavItem = { to: string; labelKey: MessageKey; icon: LucideIcon }

type NavGroup = { labelKey: MessageKey; items: NavItem[] }

export const NAV_GROUPS: NavGroup[] = [
  {
    labelKey: 'nav.group.land',
    items: [
      { to: '/farms', labelKey: 'nav.farms', icon: LandPlot },
      { to: '/fields', labelKey: 'nav.fields', icon: Square },
      { to: '/seasons', labelKey: 'nav.seasons', icon: Sprout },
    ],
  },
  {
    labelKey: 'nav.group.mapOps',
    items: [
      { to: '/map', labelKey: 'nav.map', icon: Map },
      { to: '/operations', labelKey: 'nav.operations', icon: ListChecks },
      { to: '/machines', labelKey: 'nav.machines', icon: Tractor },
      { to: '/maintenance', labelKey: 'nav.maintenance', icon: Wrench },
    ],
  },
  {
    labelKey: 'nav.group.agronomy',
    items: [{ to: '/agronomy', labelKey: 'nav.agronomy', icon: Sprout }],
  },
  {
    labelKey: 'nav.group.water',
    items: [
      { to: '/weather', labelKey: 'nav.weather', icon: CloudSun },
      { to: '/irrigation', labelKey: 'nav.irrigation', icon: Droplets },
    ],
  },
  {
    labelKey: 'nav.group.harvest',
    items: [{ to: '/harvest', labelKey: 'nav.harvest', icon: Wheat }],
  },
  {
    labelKey: 'nav.group.inventory',
    items: [{ to: '/inventory', labelKey: 'nav.inventory', icon: Package }],
  },
  {
    labelKey: 'nav.group.finance',
    items: [
      { to: '/finance', labelKey: 'nav.finance', icon: Landmark },
      { to: '/market', labelKey: 'nav.market', icon: Scale },
    ],
  },
  {
    labelKey: 'nav.group.compliance',
    items: [{ to: '/compliance', labelKey: 'nav.compliance', icon: ShieldCheck }],
  },
  {
    labelKey: 'nav.group.insights',
    items: [
      { to: '/dashboard', labelKey: 'nav.dashboard', icon: LayoutDashboard },
      { to: '/ai', labelKey: 'nav.ai', icon: Brain },
      { to: '/alerts', labelKey: 'nav.alerts', icon: Bell },
      { to: '/reports', labelKey: 'nav.reports', icon: FileSpreadsheet },
      { to: '/integrations', labelKey: 'nav.integrations', icon: Cable },
      { to: '/settings', labelKey: 'nav.settings', icon: Settings },
    ],
  },
]

export function Sidebar() {
  const collapsed = useUiStore((s) => s.sidebarCollapsed)
  const { t } = useI18n()

  return (
    <aside
      className={`flex h-full flex-col bg-pf-green text-white transition-all duration-200 ${collapsed ? 'w-[72px]' : 'w-60'}`}
    >
      <div className="flex h-16 items-center gap-2 px-4 text-sm font-semibold tracking-wide">
        <span className="inline-flex h-8 w-8 items-center justify-center rounded-[12px] bg-white/15">
          PF
        </span>
        {!collapsed ? <span>{t('chrome.brand')}</span> : null}
      </div>
      <nav className="flex-1 overflow-y-auto px-2 pb-4" aria-label={t('nav.main')}>
        {NAV_GROUPS.map((group) => (
          <div key={group.labelKey} className="mb-3">
            {!collapsed ? (
              <p className="px-3 py-1 text-[10px] font-semibold uppercase tracking-wider text-white/50">
                {t(group.labelKey)}
              </p>
            ) : null}
            {group.items.map((item) => {
              const Icon = item.icon
              const label = t(item.labelKey)
              return (
                <NavLink
                  key={item.to}
                  to={item.to}
                  title={label}
                  className={({ isActive }) =>
                    `mb-1 flex items-center gap-3 rounded-[12px] px-3 py-2 text-sm transition duration-150 ${
                      isActive ? 'bg-white/15 text-white' : 'text-white/80 hover:bg-white/10 hover:text-white'
                    }`
                  }
                >
                  <Icon className="h-5 w-5 shrink-0" aria-hidden />
                  {!collapsed ? label : <span className="sr-only">{label}</span>}
                </NavLink>
              )
            })}
          </div>
        ))}
      </nav>
    </aside>
  )
}
