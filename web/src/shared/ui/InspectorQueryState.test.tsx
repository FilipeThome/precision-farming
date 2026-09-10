import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { InspectorQueryState } from '@/shared/ui/InspectorQueryState'

describe('InspectorQueryState', () => {
  it('does not render KPI children while loading or on error', () => {
    const { rerender } = render(
      <InspectorQueryState isLoading error={null}>
        <span>12 fields</span>
      </InspectorQueryState>,
    )
    expect(screen.queryByText('12 fields')).toBeNull()
    expect(document.querySelector('[aria-busy="true"]')).toBeTruthy()

    rerender(
      <InspectorQueryState isLoading={false} error={new Error('boom')}>
        <span>12 fields</span>
      </InspectorQueryState>,
    )
    expect(screen.queryByText('12 fields')).toBeNull()
    expect(screen.getByRole('alert')).toHaveTextContent('boom')
  })
})
