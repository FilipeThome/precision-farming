import type { LucideIcon } from 'lucide-react'
import {
  Bell,
  Database,
  GitBranch,
  LandPlot,
  LayoutDashboard,
  Leaf,
  ListChecks,
  Map,
  Settings,
  Wheat,
} from 'lucide-react'
import { matchPath } from 'react-router'

import type { MessageKey } from '@/shared/i18n/useI18n'
import type { RouteTab } from '@/shared/ui/RouteTabs'

export type Domain = {
  id: string
  icon: LucideIcon
  labelKey: MessageKey
  to: string
  /** Route patterns (react-router `matchPath`) that light this domain in the rail. */
  routes: string[]
  tabs?: RouteTab[]
}

/** Navigation is data: 9 domains + Admin. Sub-tabs render in the page header via `PageHeader tabs`. */
export const DOMAINS: Domain[] = [
  { id: 'tower', icon: LayoutDashboard, labelKey: 'domain.tower', to: '/dashboard', routes: ['/dashboard'] },
  {
    id: 'farms',
    icon: LandPlot,
    labelKey: 'domain.farms',
    to: '/farms',
    routes: ['/farms', '/fields', '/seasons'],
    tabs: [
      { to: '/farms', labelKey: 'nav.farms' },
      { to: '/fields', labelKey: 'nav.fields' },
      { to: '/seasons', labelKey: 'nav.seasons' },
    ],
  },
  { id: 'map', icon: Map, labelKey: 'domain.map', to: '/map', routes: ['/map'] },
  {
    id: 'operations',
    icon: ListChecks,
    labelKey: 'domain.operations',
    to: '/operations',
    routes: ['/operations', '/machines', '/maintenance', '/inventory'],
    tabs: [
      { to: '/operations', labelKey: 'nav.operations' },
      { to: '/machines', labelKey: 'nav.machines' },
      { to: '/maintenance', labelKey: 'nav.maintenance' },
      { to: '/inventory', labelKey: 'nav.inventory' },
    ],
  },
  {
    id: 'decisions',
    icon: GitBranch,
    labelKey: 'domain.decisions',
    to: '/decisions',
    routes: ['/decisions', '/decisions/*', '/agronomy', '/irrigation', '/weather', '/ai'],
    tabs: [
      { to: '/decisions', labelKey: 'tabs.decisionQueue', end: false },
      { to: '/agronomy', labelKey: 'nav.agronomy' },
      { to: '/irrigation', labelKey: 'nav.irrigation' },
      { to: '/weather', labelKey: 'nav.weather' },
      { to: '/ai', labelKey: 'nav.ai' },
    ],
  },
  {
    id: 'data',
    icon: Database,
    labelKey: 'domain.data',
    to: '/integrations',
    routes: ['/integrations', '/reports'],
    tabs: [
      { to: '/integrations', labelKey: 'nav.integrations' },
      { to: '/reports', labelKey: 'nav.reports' },
    ],
  },
  {
    id: 'harvest',
    icon: Wheat,
    labelKey: 'domain.harvest',
    to: '/harvest',
    routes: ['/harvest', '/harvest/*'],
    tabs: [
      { to: '/harvest', labelKey: 'tabs.harvestTower' },
      { to: '/harvest/detail', labelKey: 'tabs.harvestDetail' },
    ],
  },
  {
    id: 'esg',
    icon: Leaf,
    labelKey: 'domain.esg',
    to: '/compliance',
    routes: ['/compliance', '/compliance/*', '/finance', '/market'],
    tabs: [
      { to: '/compliance', labelKey: 'nav.compliance', end: false },
      { to: '/finance', labelKey: 'nav.finance' },
      { to: '/market', labelKey: 'nav.market' },
    ],
  },
  { id: 'alerts', icon: Bell, labelKey: 'domain.alerts', to: '/alerts', routes: ['/alerts'] },
]

export const ADMIN_DOMAIN: Domain = {
  id: 'admin',
  icon: Settings,
  labelKey: 'domain.admin',
  to: '/settings',
  routes: ['/settings'],
}

export function activeDomainId(pathname: string): string | null {
  for (const domain of [...DOMAINS, ADMIN_DOMAIN]) {
    if (domain.routes.some((pattern) => matchPath({ path: pattern, end: true }, pathname))) return domain.id
  }
  return null
}

export function domainTabs(pathname: string): RouteTab[] | undefined {
  const id = activeDomainId(pathname)
  return [...DOMAINS, ADMIN_DOMAIN].find((d) => d.id === id)?.tabs
}
