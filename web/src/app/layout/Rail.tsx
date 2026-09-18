import { NavLink, useLocation } from 'react-router'

import { useAuthStore } from '@/shared/auth/store'
import { BrandLogo } from '@/shared/brand/BrandLogo'
import { useI18n } from '@/shared/i18n/useI18n'
import { preserveFarmSearch } from '@/shared/ui/RouteTabs'

import { ADMIN_DOMAIN, activeDomainId, DOMAINS, type Domain } from './domains'

function initials(name: string | null | undefined): string {
  if (!name) return '·'
  const parts = name.trim().split(/\s+/).filter(Boolean)
  const first = parts[0]?.[0] ?? ''
  const last = parts.length > 1 ? parts[parts.length - 1][0] : ''
  return (first + last).toUpperCase() || '·'
}

function RailLink({ domain, active, search }: { domain: Domain; active: boolean; search: string }) {
  const { t } = useI18n()
  const Icon = domain.icon
  const label = t(domain.labelKey)
  return (
    <NavLink
      to={`${domain.to}${preserveFarmSearch(search)}`}
      title={label}
      aria-current={active ? 'page' : undefined}
      className={`flex w-16 flex-col items-center gap-[3px] rounded-[10px] px-0 pb-1.5 pt-2 text-[10px] font-medium tracking-[0.01em] transition ${
        active
          ? 'bg-white/10 text-white shadow-[inset_3px_0_0_var(--color-ag-t-500)]'
          : 'text-[#a8c4b3] hover:bg-white/5 hover:text-white'
      }`}
    >
      <Icon className="h-5 w-5" aria-hidden />
      <span className="max-w-full truncate">{label}</span>
    </NavLink>
  )
}

/** 76px rail: 9 domains + Admin at the bottom. Active domain derived from the route, not from JSX. */
export function Rail() {
  const { pathname, search } = useLocation()
  const { t } = useI18n()
  const name = useAuthStore((s) => s.name)
  const active = activeDomainId(pathname)

  return (
    <nav
      className="flex h-full w-[76px] shrink-0 flex-col items-center gap-1 bg-[linear-gradient(180deg,var(--color-ag-g-900),var(--color-ag-g-950))] py-3 text-[#cfe3d6]"
      aria-label={t('nav.main')}
    >
      <BrandLogo
        variant="mark"
        className="mb-2.5 h-10 w-10 rounded-[12px] bg-ag-n-50 object-contain p-0.5"
      />
      {DOMAINS.map((domain) => (
        <RailLink key={domain.id} domain={domain} active={active === domain.id} search={search} />
      ))}
      <span className="flex-1" />
      <RailLink domain={ADMIN_DOMAIN} active={active === ADMIN_DOMAIN.id} search={search} />
      <span
        className="mt-1 grid h-8 w-8 place-items-center rounded-full bg-ag-g-600 text-xs font-bold text-white"
        title={name ?? undefined}
        aria-hidden
      >
        {initials(name)}
      </span>
    </nav>
  )
}
