/** Deterministic demo media paths keyed by backend DemoIds UUIDs. */
const FARM_PHOTOS: Record<string, string> = {
  'bbc017bc-be38-34d4-95df-0b1f15162e1d': '/demo/farms/farm-001.jpg',
  'edee4185-700f-3eca-bda1-5c1e350f9bd7': '/demo/farms/farm-002.jpg',
  '7b78a074-4a05-3f91-aa71-a7e40a0ae887': '/demo/farms/farm-003.jpg',
  'c4874c61-ee80-3fd1-8178-07b1c8ba3789': '/demo/farms/farm-004.jpg',
  'ec0336f8-9c22-33f4-bbad-fd1648c8c6da': '/demo/farms/farm-005.jpg',
  '0ec22ad0-3142-311b-a3f5-e1e24e868fce': '/demo/farms/farm-006.jpg',
  '41b4ef3e-ecef-333b-8575-e693335b7980': '/demo/farms/farm-007.jpg',
  '2b78b23a-0578-32f7-b999-d8e672748b4a': '/demo/farms/farm-008.jpg',
}

const MACHINE_PHOTOS: Record<string, string> = {
  '1f297fd1-d21e-3bc1-8f2e-7cc1f58261b2': '/demo/machines/machine-001.jpg',
  '9333e96c-f57d-35af-a819-98378ac30393': '/demo/machines/machine-002.jpg',
  'a553ea50-0abf-35a0-96ea-132dd25b23e0': '/demo/machines/machine-003.jpg',
  'df4e1ee3-5f7a-3391-9bf6-b285623603cf': '/demo/machines/machine-004.jpg',
  '8c0daada-661f-33a6-8210-6cf71f4c9a12': '/demo/machines/machine-005.jpg',
  '10f67c9c-309d-3088-8dfc-366b78097b51': '/demo/machines/machine-006.jpg',
  '8207052a-6b9c-3f28-9d91-17431bedd5f6': '/demo/machines/machine-007.jpg',
  'bd684837-1779-3648-b7e6-6dd979329f25': '/demo/machines/machine-008.jpg',
  '1663096f-425b-3508-8c72-38492ca7af9d': '/demo/machines/machine-009.jpg',
  '4a764502-e9d8-315e-a22b-afae790d1352': '/demo/machines/machine-010.jpg',
  'b408224c-1bd9-3a7e-a9e6-b4970fe86693': '/demo/machines/machine-011.jpg',
  'd4b460c4-37b3-321d-a601-535f9ecd8583': '/demo/machines/machine-012.jpg',
  '9861d50d-527b-385c-b89b-b0674467015f': '/demo/machines/machine-013.jpg',
  '9b2296fa-d133-37db-be2f-be69dc802915': '/demo/machines/machine-014.jpg',
}

const TYPE_FALLBACK: Record<string, string> = {
  Trator: '/demo/machines/machine-001.jpg',
  Pulverizador: '/demo/machines/machine-002.jpg',
  Colheitadeira: '/demo/machines/machine-003.jpg',
  Plantadeira: '/demo/machines/machine-005.jpg',
  Drone: '/demo/machines/machine-013.jpg',
  TRACTOR: '/demo/machines/machine-001.jpg',
  SPRAYER: '/demo/machines/machine-002.jpg',
  HARVESTER: '/demo/machines/machine-003.jpg',
  PLANTER: '/demo/machines/machine-005.jpg',
  DRONE: '/demo/machines/machine-013.jpg',
}

const CROP_PHOTOS: Record<string, string> = {
  soja: '/demo/crops/soja.jpg',
  milho: '/demo/crops/milho.jpg',
  algodão: '/demo/crops/algodao.jpg',
  algodao: '/demo/crops/algodao.jpg',
  soy: '/demo/crops/soja.jpg',
  soybean: '/demo/crops/soja.jpg',
  corn: '/demo/crops/milho.jpg',
  maize: '/demo/crops/milho.jpg',
  cotton: '/demo/crops/algodao.jpg',
}

const INVENTORY_PHOTOS: Record<string, string> = {
  GLYPHOSATE: '/demo/inventory/glyphosate.jpg',
  GLIFOSATO: '/demo/inventory/glyphosate.jpg',
  UREA: '/demo/inventory/urea.jpg',
  UREIA: '/demo/inventory/urea.jpg',
  SOY_SEED: '/demo/inventory/soy-seed.jpg',
  SOYBEAN_SEED: '/demo/inventory/soy-seed.jpg',
  DIESEL_S10: '/demo/inventory/diesel.jpg',
  DIESEL: '/demo/inventory/diesel.jpg',
  TWO_FOUR_D: '/demo/inventory/24d.jpg',
  '2_4_D': '/demo/inventory/24d.jpg',
  '24D': '/demo/inventory/24d.jpg',
  OIL_FILTER: '/demo/inventory/oil-filter.jpg',
  CORN_SEED: '/demo/inventory/corn-seed.jpg',
  MAP: '/demo/inventory/map.jpg',
  INSECTICIDE: '/demo/inventory/insecticide.jpg',
  INSETICIDA: '/demo/inventory/insecticide.jpg',
  KCL: '/demo/inventory/kcl.jpg',
  DRIVE_BELT: '/demo/inventory/drive-belt.jpg',
  COTTON_SEED: '/demo/inventory/cotton-seed.jpg',
  PRE_EMERGENT: '/demo/inventory/pre-emergent.jpg',
  HYDRAULIC_OIL: '/demo/inventory/hydraulic-oil.jpg',
}

const INVENTORY_CATEGORY_PHOTOS: Record<string, string> = {
  PESTICIDE: '/demo/inventory/insecticide.jpg',
  FERTILIZER: '/demo/inventory/urea.jpg',
  SEED: '/demo/inventory/soy-seed.jpg',
  FUEL: '/demo/inventory/diesel.jpg',
  PART: '/demo/inventory/oil-filter.jpg',
}

const IRRIGATION_PHOTOS: Record<string, string> = {
  PIVOT: '/demo/infra/pivot.jpg',
  DRIP: '/demo/infra/drip.jpg',
  SPRINKLER: '/demo/infra/sprinkler.jpg',
  PUMP: '/demo/infra/pump.jpg',
  RESERVOIR: '/demo/infra/reservoir.jpg',
}

export function farmPhoto(farmId?: string | null): string | undefined {
  if (!farmId) return undefined
  return FARM_PHOTOS[farmId] ?? '/demo/farms/farm-001.jpg'
}

export function machinePhoto(machineId?: string | null, type?: string | null): string | undefined {
  if (machineId && MACHINE_PHOTOS[machineId]) return MACHINE_PHOTOS[machineId]
  if (type && TYPE_FALLBACK[type]) return TYPE_FALLBACK[type]
  return '/demo/machines/machine-001.jpg'
}

export function cropPhoto(crop?: string | null): string {
  const key = (crop ?? '').trim().toLowerCase()
  return CROP_PHOTOS[key] ?? '/demo/crops/default.jpg'
}

export function fieldPhoto(fieldId?: string | null, crop?: string | null, farmId?: string | null): string {
  if (crop) return cropPhoto(crop)
  if (farmId && FARM_PHOTOS[farmId]) return FARM_PHOTOS[farmId]
  if (!fieldId) return '/demo/crops/default.jpg'
  const farms = Object.values(FARM_PHOTOS)
  const idx = Math.abs(hash(fieldId)) % farms.length
  return farms[idx] ?? '/demo/crops/default.jpg'
}

export function storagePhoto(type?: string | null): string {
  const key = norm(type)
  if (key.includes('WAREHOUSE')) return '/demo/infra/warehouse.jpg'
  return '/demo/infra/silo.jpg'
}

export function irrigationPhoto(type?: string | null): string {
  const key = norm(type)
  return IRRIGATION_PHOTOS[key] ?? '/demo/infra/irrigation.jpg'
}

export function logisticsPhoto(): string {
  return '/demo/infra/truck.jpg'
}

export function soilPhoto(): string {
  return '/demo/infra/soil.jpg'
}

export function scoutingPhoto(): string {
  return '/demo/infra/scouting.jpg'
}

export function weatherPhoto(): string {
  return '/demo/infra/weather.jpg'
}

export function inventoryPhoto(name?: string | null, category?: string | null): string {
  const byName = INVENTORY_PHOTOS[norm(name)]
  if (byName) return byName
  const byCategory = INVENTORY_CATEGORY_PHOTOS[norm(category)]
  if (byCategory) return byCategory
  return '/demo/infra/inventory.jpg'
}

function norm(value?: string | null): string {
  return (value ?? '')
    .trim()
    .toUpperCase()
    .replace(/[,.]/g, '')
    .replace(/[\s-]+/g, '_')
}

function hash(value: string): number {
  let h = 0
  for (let i = 0; i < value.length; i++) h = (h * 31 + value.charCodeAt(i)) | 0
  return h
}
