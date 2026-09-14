import { NavLink, useLocation } from 'react-router'

import type { MessageKey } from '@/shared/i18n/useI18n'
import { useI18n } from '@/shared/i18n/useI18n'

export type RouteTab = { to: string; labelKey: MessageKey; end?: boolean }

type RouteTabsProps = {
  tabs: RouteTab[]
  className?: string
}

/** Keeps only the farm context when moving between domain sub-pages. */
export function preserveFarmSearch(search: string): string {
  const params = new URLSearchParams(search)
  const farm = params.get('farm')
  return farm ? `?farm=${encodeURIComponent(farm)}` : ''
}

/** Route-driven sub-tabs rendered in the page header (NavLink based, preserves `?farm=`). */
export function RouteTabs({ tabs, className = '' }: RouteTabsProps) {
  const { t } = useI18n()
  const { search } = useLocation()
  const suffix = preserveFarmSearch(search)
  return (
    <nav className={`flex gap-0.5 border-b border-ag-n-200 ${className}`} aria-label={t('nav.sections')}>
      {tabs.map((tab) => (
        <NavLink
          key={tab.to}
          to={`${tab.to}${suffix}`}
          end={tab.end ?? true}
          className={({ isActive }) =>
            `-mb-px border-b-2 px-3 py-2 text-[13px] font-medium transition ${
              isActive
                ? 'border-ag-t-500 font-semibold text-ag-g-800'
                : 'border-transparent text-ag-n-600 hover:text-ag-n-900'
            }`
          }
        >
          {t(tab.labelKey)}
        </NavLink>
      ))}
    </nav>
  )
}
