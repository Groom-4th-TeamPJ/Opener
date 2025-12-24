import cn from '@/utils/cn'
import { ComponentPropsWithRef, ReactNode } from 'react'

interface ButtonProps extends ComponentPropsWithRef<'button'> {
  variant?: 'default' | 'ghost'
  size?: 'sm' | 'md' | 'lg'
  widthFull?: boolean
  isLoading?: boolean
  leftIcon?: ReactNode
  rightIcon?: ReactNode
  children?: ReactNode
}

export default function Button({
  variant = 'default',
  size = 'md',
  widthFull = false,
  isLoading = false,
  leftIcon,
  rightIcon,
  children,
  className,
  disabled,
  type = 'button',
  ...props
}: ButtonProps) {
  return (
    <button
      type={type}
      className={cn(
        'inline-flex items-center justify-center gap-2 rounded-xl font-medium cursor-pointer',
        'transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
        'disabled:pointer-events-none disabled:opacity-50',
        'relative',

        variant === 'default' && 'bg-primary text-primary-foreground hover:bg-primary/80',
        variant === 'ghost' && 'text-foreground hover:bg-muted',

        size === 'sm' && 'h-8 px-3 text-sm',
        size === 'md' && 'h-10 px-4',
        size === 'lg' && 'h-12 px-6 text-lg',

        widthFull && 'w-full',

        className
      )}
      disabled={disabled || isLoading}
      {...props}
    >
      {isLoading && (
        <span
          className={cn(
            'absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2',
            'inline-block rounded-full border-2 border-current border-t-transparent animate-spin',
            size === 'sm' && 'w-3 h-3',
            size === 'md' && 'w-4 h-4',
            size === 'lg' && 'w-5 h-5'
          )}
        />
      )}
      <span className={cn(isLoading && 'invisible')}>
        {leftIcon && <span className="shrink-0">{leftIcon}</span>}
        {children}
        {rightIcon && <span className="shrink-0">{rightIcon}</span>}
      </span>
    </button>
  )
}
