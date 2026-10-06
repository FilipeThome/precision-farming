import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'

import type { EvidencePack as EvidencePackDto } from '@/features/compliance/types'

import { EvidencePack, evidenceDownloadName } from './EvidencePack'

const pack: EvidencePackDto = {
  lotCode: 'LOT-BV-001',
  farmId: 'farm-1',
  farmName: 'Boa Vista',
  fieldId: 'field-1',
  fieldName: 'Talhão 1',
  polygonGeoJson: { type: 'Polygon', coordinates: [[[0, 0], [1, 0], [1, 1], [0, 0]]] },
  inputRefs: ['inp-1'],
  receituarioNumber: 'REC-9',
  activeIngredient: 'glyphosate',
  moaGroup: 'G',
  responsibleTechCpf: '111.111.111-11',
  phiDays: 14,
  deforestationCutoffDate: '2008-07-22',
  embargoed: false,
  carStatus: 'ACTIVE',
  simulation: true,
}

vi.mock('@/features/compliance/queries', () => ({
  useEvidencePackQuery: () => ({
    data: pack,
    isLoading: false,
    isError: false,
    error: null,
    refetch: vi.fn(),
  }),
}))

describe('evidenceDownloadName', () => {
  it('strips path characters from the lot code', () => {
    expect(evidenceDownloadName('../etc/passwd')).toBe('evidence-etcpasswd.json')
    expect(evidenceDownloadName('***')).toBe('evidence.json')
    expect(evidenceDownloadName('LOT-BV-001')).toBe('evidence-LOT-BV-001.json')
  })
})

describe('EvidencePack', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('download uses the response JSON; print spies window.print', async () => {
    const user = userEvent.setup()
    const printSpy = vi.spyOn(window, 'print').mockImplementation(() => undefined)
    let captured: Blob | undefined
    const createObjectURL = vi.fn((blob: Blob) => {
      captured = blob
      return 'blob:evidence'
    })
    const revokeObjectURL = vi.fn()
    vi.stubGlobal('URL', { ...URL, createObjectURL, revokeObjectURL })

    // Avoid jsdom navigation on <a download> click.
    const clickSpy = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined)

    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(
      <QueryClientProvider client={client}>
        <EvidencePack lotCode="LOT-BV-001" />
      </QueryClientProvider>,
    )

    await user.click(screen.getByRole('button', { name: 'Baixar JSON' }))
    expect(createObjectURL).toHaveBeenCalled()
    expect(captured).toBeInstanceOf(Blob)
    expect(captured!.type).toBe('application/json')
    const text = await new Promise<string>((resolve, reject) => {
      const reader = new FileReader()
      reader.onload = () => resolve(String(reader.result))
      reader.onerror = () => reject(reader.error)
      reader.readAsText(captured!)
    })
    expect(JSON.parse(text)).toEqual(pack)
    expect(clickSpy).toHaveBeenCalled()

    await user.click(screen.getByRole('button', { name: 'Imprimir' }))
    expect(printSpy).toHaveBeenCalled()
  })
})
