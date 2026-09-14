import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { ConfidenceMeter, confidenceLevel } from '@/shared/ui/ConfidenceMeter'

describe('ConfidenceMeter', () => {
  it('maps values to levels', () => {
    expect(confidenceLevel(0.79)).toBe('hi')
    expect(confidenceLevel(0.75)).toBe('hi')
    expect(confidenceLevel(0.62)).toBe('md')
    expect(confidenceLevel(0.41)).toBe('lo')
  })

  it('renders nothing without a value', () => {
    const { container } = render(<ConfidenceMeter value={null} />)
    expect(container).toBeEmptyDOMElement()
  })

  it('renders the number and the localized level', () => {
    render(<ConfidenceMeter value={0.79} />)
    expect(screen.getByRole('meter')).toHaveAttribute('aria-valuenow', '0.79')
    expect(screen.getByText('0.79')).toBeInTheDocument()
    expect(screen.getByText('Confiança alta')).toBeInTheDocument()
  })
})
