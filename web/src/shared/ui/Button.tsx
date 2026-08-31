import type { ButtonHTMLAttributes, ReactNode } from 'react'

type Variant = 'primary' | 'secondary' | 'ghost' | 'danger'

const variants: Record<Variant, string> = {
  primary: 'bg-pf-green text-white hover:bg-pf-green-dark',
  secondary: 'bg-white text-pf-green border border-pf-border hover:border-pf-teal',
  ghost: 'bg-transparent text-pf-green hover:bg-white/60',
  danger: 'bg-red-700 text-white hover:bg-red-800',
}

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: Variant
  children: ReactNode
}

export function Button({ variant = 'primary', className = '', children, type, ...props }: ButtonProps) {
  return (
    <button
      type={type ?? 'button'}
      className={`inline-flex items-center justify-center gap-2 rounded-[12px] px-3 py-2 text-sm font-medium transition duration-150 disabled:cursor-not-allowed disabled:opacity-50 ${variants[variant]} ${className}`}
      {...props}
    >
      {children}
    </button>
  )
}
