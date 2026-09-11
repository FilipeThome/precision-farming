import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { FreshnessChip } from '@/shared/ui/FreshnessChip'

const NOW = Date.parse('2026-09-10T12:00:00Z')

describe('FreshnessChip', () => {
  it('renders nothing without a timestamp', () => {
    const { container } = render(<FreshnessChip at={null} now={NOW} />)
    expect(container).toBeEmptyDOMElement()
  })

  it('shows the age and flags stale data past the threshold', () => {
    render(<FreshnessChip at="2026-09-10T10:00:00Z" now={NOW} />)
    expect(screen.getByText('2h')).toHaveAttribute('data-stale', 'false')
  })

  it('uses the caller threshold', () => {
    render(<FreshnessChip at="2026-09-09T00:00:00Z" now={NOW} staleAfterMs={6 * 3_600_000} />)
    expect(screen.getByText('36h')).toHaveAttribute('data-stale', 'true')
  })
})
