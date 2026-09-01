import { lazy, Suspense } from 'react'
import { Navigate, Outlet, Route, Routes } from 'react-router'

import { AppShell } from '@/app/layout/AppShell'
import { useI18n } from '@/shared/i18n/useI18n'
import { useAuthStore } from '@/shared/auth/store'

const LoginPage = lazy(() =>
  import('@/features/auth/pages/LoginPage').then((m) => ({ default: m.LoginPage })),
)
const DashboardPage = lazy(() =>
  import('@/features/dashboard/pages/DashboardPage').then((m) => ({ default: m.DashboardPage })),
)
const MapPage = lazy(() => import('@/features/map/pages/MapPage').then((m) => ({ default: m.MapPage })))
const FarmsPage = lazy(() =>
  import('@/features/farms/pages/FarmsPage').then((m) => ({ default: m.FarmsPage })),
)
const FieldsPage = lazy(() =>
  import('@/features/fields/pages/FieldsPage').then((m) => ({ default: m.FieldsPage })),
)
const SeasonsPage = lazy(() =>
  import('@/features/seasons/pages/SeasonsPage').then((m) => ({ default: m.SeasonsPage })),
)
const MachinesPage = lazy(() =>
  import('@/features/machines/pages/MachinesPage').then((m) => ({ default: m.MachinesPage })),
)
const OperationsPage = lazy(() =>
  import('@/features/operations/pages/OperationsPage').then((m) => ({ default: m.OperationsPage })),
)
const MaintenancePage = lazy(() =>
  import('@/features/maintenance/pages/MaintenancePage').then((m) => ({ default: m.MaintenancePage })),
)
const AgronomyPage = lazy(() =>
  import('@/features/agronomy/pages/AgronomyPage').then((m) => ({ default: m.AgronomyPage })),
)
const InventoryPage = lazy(() =>
  import('@/features/inventory/pages/InventoryPage').then((m) => ({ default: m.InventoryPage })),
)
const WeatherPage = lazy(() =>
  import('@/features/weather/pages/WeatherPage').then((m) => ({ default: m.WeatherPage })),
)
const IrrigationPage = lazy(() =>
  import('@/features/irrigation/pages/IrrigationPage').then((m) => ({ default: m.IrrigationPage })),
)
const HarvestPage = lazy(() =>
  import('@/features/harvest/pages/HarvestPage').then((m) => ({ default: m.HarvestPage })),
)
const FinancePage = lazy(() =>
  import('@/features/finance/pages/FinancePage').then((m) => ({ default: m.FinancePage })),
)
const MarketPage = lazy(() =>
  import('@/features/market/pages/MarketPage').then((m) => ({ default: m.MarketPage })),
)
const CompliancePage = lazy(() =>
  import('@/features/compliance/pages/CompliancePage').then((m) => ({ default: m.CompliancePage })),
)
const TraceabilityLotPage = lazy(() =>
  import('@/features/compliance/pages/TraceabilityLotPage').then((m) => ({
    default: m.TraceabilityLotPage,
  })),
)
const IntegrationsPage = lazy(() =>
  import('@/features/integrations/pages/IntegrationsPage').then((m) => ({
    default: m.IntegrationsPage,
  })),
)
const AiInsightsPage = lazy(() =>
  import('@/features/ai/pages/AiInsightsPage').then((m) => ({ default: m.AiInsightsPage })),
)
const AlertsPage = lazy(() =>
  import('@/features/alerts/pages/AlertsPage').then((m) => ({ default: m.AlertsPage })),
)
const ReportsPage = lazy(() =>
  import('@/features/reports/pages/ReportsPage').then((m) => ({ default: m.ReportsPage })),
)
const SettingsPage = lazy(() =>
  import('@/features/settings/pages/SettingsPage').then((m) => ({ default: m.SettingsPage })),
)

function Fallback() {
  const { t } = useI18n()
  return (
    <div className="flex min-h-64 items-center justify-center text-sm text-pf-muted">{t('chrome.loading')}</div>
  )
}

function ProtectedLayout() {
  const token = useAuthStore((s) => s.accessToken)
  if (!token) return <Navigate to="/login" replace />
  return <AppShell />
}

function GuestOnly() {
  const token = useAuthStore((s) => s.accessToken)
  if (token) return <Navigate to="/dashboard" replace />
  return <Outlet />
}

export function AppRouter() {
  return (
    <Suspense fallback={<Fallback />}>
      <Routes>
        <Route element={<GuestOnly />}>
          <Route path="/login" element={<LoginPage />} />
        </Route>
        <Route element={<ProtectedLayout />}>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/map" element={<MapPage />} />
          <Route path="/farms" element={<FarmsPage />} />
          <Route path="/fields" element={<FieldsPage />} />
          <Route path="/seasons" element={<SeasonsPage />} />
          <Route path="/machines" element={<MachinesPage />} />
          <Route path="/operations" element={<OperationsPage />} />
          <Route path="/maintenance" element={<MaintenancePage />} />
          <Route path="/agronomy" element={<AgronomyPage />} />
          <Route path="/inventory" element={<InventoryPage />} />
          <Route path="/weather" element={<WeatherPage />} />
          <Route path="/irrigation" element={<IrrigationPage />} />
          <Route path="/harvest" element={<HarvestPage />} />
          <Route path="/finance" element={<FinancePage />} />
          <Route path="/market" element={<MarketPage />} />
          <Route path="/compliance" element={<CompliancePage />} />
          <Route path="/compliance/lots/:lotCode" element={<TraceabilityLotPage />} />
          <Route path="/ai" element={<AiInsightsPage />} />
          <Route path="/alerts" element={<AlertsPage />} />
          <Route path="/reports" element={<ReportsPage />} />
          <Route path="/integrations" element={<IntegrationsPage />} />
          <Route path="/settings" element={<SettingsPage />} />
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Route>
      </Routes>
    </Suspense>
  )
}
