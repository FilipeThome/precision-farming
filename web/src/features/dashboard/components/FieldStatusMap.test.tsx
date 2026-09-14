import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import { FieldStatusMap } from '@/features/dashboard/components/FieldStatusMap'
import type { Field } from '@/shared/api/types'

vi.mock('@/shared/maps/FieldMap', () => ({
  FieldMap: ({ fieldStates }: { fieldStates?: Record<string, string> }) => (
    <div data-testid="field-map" data-states={JSON.stringify(fieldStates ?? {})} />
  ),
}))

const field: Field = {
  id: 'field-1',
  farmId: 'farm-1',
  name: 'Talhão 01',
  areaHa: 10,
  crop: 'SOY',
  variety: null,
  geometry: '',
}

describe('FieldStatusMap', () => {
  it('renders a white status chip and two-column legend classes', () => {
    const { container } = render(
      <FieldStatusMap fields={[field]} fieldStates={{}} isLoading={false} isError={false} />,
    )
    expect(screen.getByTestId('field-map')).toBeInTheDocument()
    const chip = screen.getByText('Status dos talhões').closest('span')
    expect(chip).toHaveClass('bg-white')
    expect(chip).not.toHaveClass('bg-ag-g-900')
    expect(chip?.parentElement).toHaveClass('left-[46px]')
    const legend = container.querySelector('dl')
    expect(legend).toHaveClass('bottom-7')
    expect(screen.getAllByText('Concluído').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Em andamento').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Planejado').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Pausado').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Sem operação').length).toBeGreaterThan(0)
  })

  it('shows a busy placeholder while loading', () => {
    render(<FieldStatusMap fields={[field]} fieldStates={{}} isLoading isError={false} />)
    expect(screen.getByRole('status', { busy: true })).toBeInTheDocument()
    expect(screen.queryByTestId('field-map')).not.toBeInTheDocument()
  })

  it('shows empty copy when there are no fields', () => {
    render(<FieldStatusMap fields={[]} fieldStates={{}} isLoading={false} isError={false} />)
    expect(screen.getByText('Nenhum talhão encontrado')).toBeInTheDocument()
    expect(screen.queryByTestId('field-map')).not.toBeInTheDocument()
  })

  it('shows the error, correlation id, and retries when asked', async () => {
    const user = userEvent.setup()
    const onRetry = vi.fn()
    render(
      <FieldStatusMap
        fields={[field]}
        fieldStates={{}}
        isLoading={false}
        isError
        errorMessage="fields down"
        correlationId="cid-fields"
        onRetry={onRetry}
      />,
    )
    expect(screen.getByRole('alert')).toHaveTextContent('Não foi possível carregar os dados')
    expect(screen.getByText('fields down')).toBeInTheDocument()
    expect(screen.getByText('cid-fields')).toBeInTheDocument()
    expect(screen.queryByTestId('field-map')).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Tentar novamente' }))
    expect(onRetry).toHaveBeenCalledOnce()
  })
})
