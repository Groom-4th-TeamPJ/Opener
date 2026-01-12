import cn from '@/utils/cn'
import { ComponentPropsWithRef, ReactNode } from 'react'

interface ButtonProps extends ComponentPropsWithRef<'button'> {
  variant?: 'default' | 'secondary' | 'ghost' | 'outline'
  size?: 'sm' | 'md' | 'lg'
  widthFull?: boolean
  isLoading?: boolean
  leftIcon?: ReactNode
  rightIcon?: ReactNode
  bothIcon?: { left: ReactNode; right: ReactNode }
  children?: ReactNode
}

export default function Button({
  variant = 'default',
  size = 'md',
  widthFull = false,
  isLoading = false,
  leftIcon,
  rightIcon,
  bothIcon,
  children,
  className,
  disabled,
  type = 'button',
  ...props
}: ButtonProps) {
  const finalLeftIcon = bothIcon?.left ?? leftIcon
  const finalRightIcon = bothIcon?.right ?? rightIcon
  return (
    <button
      type={type}
      className={cn(
        'inline-flex items-center justify-center gap-2 rounded-lg cursor-pointer font-bold',
        'transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
        'disabled:pointer-events-none disabled:text-neutral-200 disabled:shadow-1',
        'relative',

        variant === 'default' &&
          'bg-primary-600 text-background hover:bg-primary-500 active:bg-primary-700 disabled:bg-neutral-50',
        variant === 'secondary' &&
          'bg-neutral-200 text-text-primary hover:bg-neutral-100 active:bg-neutral-300 disabled:bg-neutral-50',
        variant === 'ghost' && 'text-text-primary hover:bg-neutral-50',
        variant === 'outline' &&
          'border border-primary-600 text-text-primary hover:border-primary-500 active:border-primary-700 disabled:border-neutral-50',

        size === 'sm' && 'h-8 px-3 text-sm',
        size === 'md' && 'h-10 px-4',
        size === 'lg' && 'h-12 px-6',

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
      <span className={cn('flex items-center gap-1', isLoading && 'invisible')}>
        {finalLeftIcon && <span className="shrink-0">{finalLeftIcon}</span>}
        {children}
        {finalRightIcon && <span className="shrink-0">{finalRightIcon}</span>}
      </span>
    </button>
  )
}
