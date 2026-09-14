import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { BrandLogo } from './BrandLogo'

describe('BrandLogo', () => {
  it('renders the leaf mark with the brand name as alt', () => {
    render(<BrandLogo />)
    const img = screen.getByRole('img', { name: 'Precision Farming' })
    expect(img).toHaveAttribute('src', '/brand/precision-mark.svg')
  })

  it('keeps the mark decorative when a heading already names the brand', () => {
    render(<BrandLogo decorative />)
    const img = document.querySelector('img')
    expect(img).toHaveAttribute('alt', '')
    expect(img).toHaveAttribute('src', '/brand/precision-mark.svg')
  })

  it('points the wordmark variant at the static PNG', () => {
    render(<BrandLogo variant="wordmark" />)
    expect(screen.getByRole('img', { name: 'Precision Farming' })).toHaveAttribute(
      'src',
      '/brand/precision-wordmark.png',
    )
  })
})
