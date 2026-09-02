import { useState } from 'react'

type EntityPhotoProps = {
  src?: string
  alt: string
  className?: string
  /** Full card strip vs compact thumb (tables / lists). */
  variant?: 'hero' | 'thumb'
}

export function EntityPhoto({ src, alt, className = '', variant = 'hero' }: EntityPhotoProps) {
  const [failed, setFailed] = useState(false)
  const shape =
    variant === 'thumb'
      ? 'h-12 w-16 shrink-0 rounded-md object-cover'
      : 'aspect-video w-full rounded-t-[12px] object-cover'

  if (!src || failed) {
    return (
      <div
        className={`${variant === 'thumb' ? 'h-12 w-16 shrink-0 rounded-md' : 'aspect-video w-full rounded-t-[12px]'} bg-gradient-to-br from-pf-green/20 to-pf-teal/10 ${className}`}
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
