import { useAuthStore } from '@/shared/auth/store'

const MANAGER_ROLES = new Set(['ADMIN', 'FARM_MANAGER'])

export function canApprovePrescriptions(role: string | null | undefined): boolean {
  return role != null && MANAGER_ROLES.has(role)
}

export function canDispatchLoads(role: string | null | undefined): boolean {
  return role != null && MANAGER_ROLES.has(role)
}

export function useCanManageFarmOps(): boolean {
  return canApprovePrescriptions(useAuthStore((s) => s.role))
}
