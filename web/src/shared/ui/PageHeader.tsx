import type { ReactNode } from 'react'
import { useInRouterContext, useLocation } from 'react-router'

import { domainTabs } from '@/app/layout/domains'
import { RouteTabs, type RouteTab } from '@/shared/ui/RouteTabs'

type PageHeaderProps = {
  title: string
  description?: string
  actions?: ReactNode
  /** Inline tags rendered next to the title (e.g. date, weather window). */
  meta?: ReactNode
  /**
   * Route-driven sub-tabs. Defaults to the active domain's tabs from `domains.ts`;
   * pass `[]` to hide them or an explicit list to override.
   */
  tabs?: RouteTab[]
}

function DomainTabs() {
  const { pathname } = useLocation()
  const tabs = domainTabs(pathname)
  return tabs && tabs.length > 0 ? <RouteTabs tabs={tabs} /> : null
}

export function PageHeader({ title, description, actions, meta, tabs }: PageHeaderProps) {
  const inRouter = useInRouterContext()
  return (
    <header className="mb-4 flex flex-col gap-2.5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="font-display text-[22px] font-bold text-ag-n-900">{title}</h1>
            {meta}
          </div>
          {description ? <p className="mt-1 text-[13px] text-ag-n-600">{description}</p> : null}
        </div>
        {actions}
      </div>
      {tabs ? tabs.length > 0 ? <RouteTabs tabs={tabs} /> : null : inRouter ? <DomainTabs /> : null}
    </header>
  )
}
