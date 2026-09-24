import { useMutation, useQueries, useQuery, useQueryClient } from '@tanstack/react-query'

import {
  approvePrescription,
  fetchMoaRotation,
  fetchPrescription,
  fetchPrescriptions,
  fetchRecommendations,
  fetchScouting,
  fetchSoilSamples,
  fetchSpraySavings,
  rejectPrescription,
} from './api'

type QueryToggle = { enabled?: boolean }

export const agronomyKeys = {
  scouting: (farmId?: string | null) => ['agronomy', 'scouting', farmId ?? 'all'] as const,
  soil: (farmId?: string | null) => ['agronomy', 'soil', farmId ?? 'all'] as const,
  recommendations: (farmId?: string | null) =>
    ['agronomy', 'recommendations', farmId ?? 'all'] as const,
  prescriptions: (farmId?: string | null) =>
    ['agronomy', 'prescriptions', farmId ?? 'all'] as const,
  prescription: (id: string) => ['agronomy', 'prescription', id] as const,
  spraySavings: (prescriptionId: string) =>
    ['agronomy', 'spray-savings', prescriptionId] as const,
  moaRotation: (fieldId: string) => ['agronomy', 'moa-rotation', fieldId] as const,
}

export function useScoutingQuery(farmId?: string | null, options?: QueryToggle) {
  return useQuery({
    queryKey: agronomyKeys.scouting(farmId),
    queryFn: () => fetchScouting(farmId),
    staleTime: 30_000,
    enabled: options?.enabled ?? true,
  })
}

export function useSoilSamplesQuery(farmId?: string | null, options?: QueryToggle) {
  return useQuery({
    queryKey: agronomyKeys.soil(farmId),
    queryFn: () => fetchSoilSamples(farmId),
    staleTime: 30_000,
    enabled: options?.enabled ?? true,
  })
}

export function useRecommendationsQuery(farmId?: string | null, options?: QueryToggle) {
  return useQuery({
    queryKey: agronomyKeys.recommendations(farmId),
    queryFn: () => fetchRecommendations(farmId),
    staleTime: 30_000,
    enabled: options?.enabled ?? true,
  })
}

export function usePrescriptionsQuery(farmId?: string | null, options?: QueryToggle) {
  return useQuery({
    queryKey: agronomyKeys.prescriptions(farmId),
    queryFn: () => fetchPrescriptions(farmId),
    staleTime: 30_000,
    enabled: options?.enabled ?? true,
  })
}

export function usePrescriptionQuery(id?: string | null, options?: QueryToggle) {
  return useQuery({
    queryKey: agronomyKeys.prescription(id ?? ''),
    queryFn: () => fetchPrescription(id!),
    staleTime: 30_000,
    enabled: (options?.enabled ?? true) && Boolean(id),
  })
}

export function useSpraySavingsQuery(prescriptionId?: string | null, options?: QueryToggle) {
  return useQuery({
    queryKey: agronomyKeys.spraySavings(prescriptionId ?? ''),
    queryFn: () => fetchSpraySavings(prescriptionId!),
    staleTime: 30_000,
    enabled: (options?.enabled ?? true) && Boolean(prescriptionId),
  })
}

export function useMoaRotationQuery(fieldId?: string | null, options?: QueryToggle) {
  return useQuery({
    queryKey: agronomyKeys.moaRotation(fieldId ?? ''),
    queryFn: () => fetchMoaRotation(fieldId!),
    staleTime: 30_000,
    enabled: (options?.enabled ?? true) && Boolean(fieldId),
  })
}

/** Parallel MoA checks for the distinct field ids already known from the prescriptions list. */
export function useMoaRotationQueries(fieldIds: string[], options?: QueryToggle) {
  return useQueries({
    queries: fieldIds.map((fieldId) => ({
      queryKey: agronomyKeys.moaRotation(fieldId),
      queryFn: () => fetchMoaRotation(fieldId),
      staleTime: 30_000,
      enabled: (options?.enabled ?? true) && Boolean(fieldId),
    })),
  })
}

function invalidateAfterPrescriptionChange(client: ReturnType<typeof useQueryClient>) {
  void client.invalidateQueries({ queryKey: ['agronomy', 'prescriptions'] })
  void client.invalidateQueries({ queryKey: ['agronomy', 'prescription'] })
  void client.invalidateQueries({ queryKey: ['agronomy', 'spray-savings'] })
  void client.invalidateQueries({ queryKey: ['agronomy', 'moa-rotation'] })
}

export function useApprovePrescription() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => approvePrescription(id),
    onSuccess: () => invalidateAfterPrescriptionChange(client),
  })
}

export function useRejectPrescription() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => rejectPrescription(id),
    onSuccess: () => invalidateAfterPrescriptionChange(client),
  })
}
