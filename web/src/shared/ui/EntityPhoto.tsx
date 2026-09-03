import { useState } from 'react'

type EntityPhotoProps = {
  src?: string
  alt: string
  className?: string
  /** Medium thumbnail (cards) vs compact thumb (dense lists). */
  variant?: 'medium' | 'thumb'
}

const SHAPE: Record<NonNullable<EntityPhotoProps['variant']>, string> = {
  medium: 'h-24 w-32 shrink-0 rounded-lg object-cover',
  thumb: 'h-14 w-20 shrink-0 rounded-md object-cover',
}

export function EntityPhoto({ src, alt, className = '', variant = 'medium' }: EntityPhotoProps) {
  const [failed, setFailed] = useState(false)
  const shape = SHAPE[variant]

  if (!src || failed) {
    return (
      <div
        className={`${shape} bg-gradient-to-br from-pf-green/20 to-pf-teal/10 ${className}`}
        aria-hidden
      />
    )
  }
  return (
    <img
      src={src}
      alt={alt}
      loading="lazy"
      onError={() => setFailed(true)}
      className={`${shape} ${className}`}
    />
  )
}
