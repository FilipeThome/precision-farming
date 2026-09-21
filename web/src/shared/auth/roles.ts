import { useAuthStore } from '@/shared/auth/store'

const MANAGER_ROLES = new Set(['ADMIN', 'FARM_MANAGER'])
const FLEET_ROLES = new Set(['ADMIN', 'FARM_MANAGER', 'MAINTENANCE'])

export function canApprovePrescriptions(role: string | null | undefined): boolean {
  return role != null && MANAGER_ROLES.has(role)
}

export function canDispatchLoads(role: string | null | undefined): boolean {
  return role != null && MANAGER_ROLES.has(role)
}

export function canCreateFarm(role: string | null | undefined): boolean {
  return role === 'ADMIN'
}

export function canWriteMasterData(role: string | null | undefined): boolean {
  return role != null && MANAGER_ROLES.has(role)
}

export function canWriteFleet(role: string | null | undefined): boolean {
  return role != null && FLEET_ROLES.has(role)
}

export function useCanManageFarmOps(): boolean {
  return canApprovePrescriptions(useAuthStore((s) => s.role))
}

export function useCanCreateFarm(): boolean {
  return canCreateFarm(useAuthStore((s) => s.role))
}

export function useCanWriteMasterData(): boolean {
  return canWriteMasterData(useAuthStore((s) => s.role))
}

export function useCanWriteFleet(): boolean {
  return canWriteFleet(useAuthStore((s) => s.role))
}
