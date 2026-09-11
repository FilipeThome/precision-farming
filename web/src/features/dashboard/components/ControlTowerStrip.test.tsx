import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'

import { ControlTowerStrip } from '@/features/dashboard/components/ControlTowerStrip'
import type { Alert, Machine, Operation } from '@/shared/api/types'

const alert: Alert = {
  id: 'a1',
  farmId: 'farm-1',
  severity: 'HIGH',
  type: 'WEATHER',
  title: 'Rain',
  message: 'Storm',
  entityType: null,
  entityId: null,
  status: 'OPEN',
  createdAt: '2026-09-01T00:00:00Z',
}

const machine: Machine = {
  id: 'm1',
  farmId: 'farm-2',
  name: 'Tractor',
  type: 'TRACTOR',
  manufacturer: 'X',
  model: 'Y',
  status: 'OPERATING',
}

const operation: Operation = {
  id: 'o1',
  fieldId: 'f1',
  farmId: 'farm-3',
  type: 'PLANTING',
  status: 'IN_PROGRESS',
  plannedStart: null,
  plannedEnd: null,
  actualStart: null,
  actualEnd: null,
  machineId: null,
  pauseReason: null,
  itemId: null,
  itemQuantity: null,
  areaHa: null,
}

describe('ControlTowerStrip', () => {
  it('includes farm on selected links', () => {
    render(
      <MemoryRouter>
        <ControlTowerStrip alerts={[alert]} machines={[machine]} operations={[operation]} />
      </MemoryRouter>,
    )
    const hrefs = screen.getAllByRole('link').map((el) => el.getAttribute('href'))
    expect(hrefs).toContain('/alerts?selected=a1&farm=farm-1')
    expect(hrefs).toContain('/machines?selected=m1&farm=farm-2')
    expect(hrefs).toContain('/operations?selected=o1&farm=farm-3')
  })
})
