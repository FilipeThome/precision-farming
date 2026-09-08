import type { HTMLAttributes, ReactNode } from 'react'

type CardProps = HTMLAttributes<HTMLDivElement> & {
  children: ReactNode
}

export function Card({ children, className = '', ...props }: CardProps) {
  return (
    <div
      className={`rounded-[12px] border border-pf-border bg-white p-4 shadow-sm ${className}`}
      {...props}
    >
      {children}
    </div>
  )
}
