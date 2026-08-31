import type { ReactNode } from 'react'

type CardProps = {
  children: ReactNode
  className?: string
}

export function Card({ children, className = '' }: CardProps) {
  return (
    <div className={`rounded-[12px] border border-pf-border bg-white p-4 shadow-sm ${className}`}>
      {children}
    </div>
  )
}
