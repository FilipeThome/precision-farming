import type { HTMLAttributes, ReactNode } from 'react'

type CardProps = HTMLAttributes<HTMLDivElement> & {
  children: ReactNode
}

export function Card({ children, className = '', ...props }: CardProps) {
  return (
    <div
      className={`rounded-[14px] border border-ag-n-200 bg-ag-n-0 p-4 shadow-[0_1px_2px_rgba(20,30,25,0.06),0_1px_0_rgba(20,30,25,0.04)] ${className}`}
      {...props}
    >
      {children}
    </div>
  )
}
