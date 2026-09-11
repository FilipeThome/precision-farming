import type { ButtonHTMLAttributes, ReactNode } from 'react'

type Variant = 'primary' | 'secondary' | 'ghost' | 'danger'
type Size = 'sm' | 'md' | 'lg'

const variants: Record<Variant, string> = {
  primary: 'bg-ag-t-600 text-white shadow-[inset_0_-1px_0_rgba(0,0,0,0.15)] hover:bg-ag-t-700',
  secondary: 'bg-ag-n-0 text-ag-g-800 border border-ag-n-300 hover:border-ag-t-500',
  ghost: 'bg-transparent text-ag-n-700 hover:bg-ag-n-100',
  danger: 'bg-ag-n-0 text-ag-crit border border-[#f2b8b5] hover:bg-ag-crit-bg',
}

const sizes: Record<Size, string> = {
  sm: 'min-h-11 px-2.5 py-1.5 text-xs',
  md: 'min-h-11 px-3 py-2 text-[13px]',
  lg: 'min-h-12 px-4 py-3 text-base rounded-[12px]',
}

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: Variant
  size?: Size
  children: ReactNode
}

export function Button({ variant = 'primary', size = 'md', className = '', children, type, ...props }: ButtonProps) {
  return (
    <button
      type={type ?? 'button'}
      className={`inline-flex items-center justify-center gap-1.5 rounded-[8px] font-semibold leading-none transition duration-150 disabled:cursor-not-allowed disabled:opacity-50 ${sizes[size]} ${variants[variant]} ${className}`}
      {...props}
    >
      {children}
    </button>
  )
}
