import type { LucideIcon } from 'lucide-react'
import {
  Bell,
  Bot,
  Bug,
  CloudSun,
  Droplets,
  FlaskConical,
  GitBranch,
  LandPlot,
  Lightbulb,
  ListChecks,
  Package,
  Sprout,
  Square,
  Tractor,
  Truck,
  Warehouse,
  Wheat,
} from 'lucide-react'

export type EntityKind =
  | 'farm'
  | 'field'
  | 'machine'
  | 'crop'
  | 'inventory'
  | 'irrigation'
  | 'storage'
  | 'truck'
  | 'weather'
  | 'soil'
  | 'scouting'
  | 'alert'
  | 'operation'
  | 'season'
  | 'recommendation'
  | 'decision'

export type EntityTone = 'brand' | 'teal' | 'warn' | 'crit' | 'info' | 'neutral'
export type EntityTileSize = 'sm' | 'md' | 'lg'

const KIND_ICON: Record<EntityKind, LucideIcon> = {
  farm: LandPlot,
  field: Square,
  machine: Tractor,
  crop: Wheat,
  inventory: Package,
  irrigation: Droplets,
  storage: Warehouse,
  truck: Truck,
  weather: CloudSun,
  soil: FlaskConical,
  scouting: Bug,
  alert: Bell,
  operation: ListChecks,
  season: Sprout,
  recommendation: Lightbulb,
  decision: GitBranch,
}

const MACHINE_ICON: Record<string, LucideIcon> = {
  TRACTOR: Tractor,
  SPRAYER: Droplets,
  HARVESTER: Wheat,
  PLANTER: Sprout,
  DRONE: Bot,
}

const TONE: Record<EntityTone, string> = {
  brand: 'bg-ag-g-100 text-ag-g-700',
  teal: 'bg-ag-t-100 text-ag-t-700',
  warn: 'bg-ag-warn-bg text-ag-warn',
  crit: 'bg-ag-crit-bg text-ag-crit',
  info: 'bg-ag-info-bg text-ag-info',
  neutral: 'bg-ag-n-100 text-ag-n-700',
}

const SIZE: Record<EntityTileSize, { box: string; icon: string }> = {
  sm: { box: 'h-9 w-9 rounded-[10px]', icon: 'h-4 w-4' },
  md: { box: 'h-12 w-12 rounded-[12px]', icon: 'h-5 w-5' },
  lg: { box: 'h-16 w-16 rounded-[14px]', icon: 'h-7 w-7' },
}

type EntityTileProps = {
  kind: EntityKind
  /** Machine type code (TRACTOR, SPRAYER…) refines the icon for `kind="machine"`. */
  machineType?: string | null
  tone?: EntityTone
  size?: EntityTileSize
  className?: string
  /** Accessible name; when omitted the tile is decorative. */
  label?: string
  /** Authenticated blob URL; when set, replaces the icon. */
  photoUrl?: string | null
}

export function entityIcon(kind: EntityKind, machineType?: string | null): LucideIcon {
  if (kind === 'machine' && machineType) {
    const key = machineType.trim().toUpperCase()
    if (MACHINE_ICON[key]) return MACHINE_ICON[key]
  }
  return KIND_ICON[kind]
}

/** Icon tile on a tinted rounded square — replaces photos for every entity. */
export function EntityTile({
  kind,
  machineType,
  tone = 'brand',
  size = 'md',
  className = '',
  label,
  photoUrl,
}: EntityTileProps) {
  const Icon = entityIcon(kind, machineType)
  const s = SIZE[size]
  return (
    <span
      className={`inline-grid shrink-0 place-items-center overflow-hidden ${s.box} ${photoUrl ? 'bg-ag-n-100' : TONE[tone]} ${className}`}
      role={label ? 'img' : undefined}
      aria-label={label}
      aria-hidden={label ? undefined : true}
      data-kind={kind}
    >
      {photoUrl ? (
        <img src={photoUrl} alt="" className="h-full w-full object-cover" />
      ) : (
        <Icon className={s.icon} aria-hidden />
      )}
    </span>
  )
}
