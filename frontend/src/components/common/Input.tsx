import cn from '@/utils/cn'
import { ComponentPropsWithRef, ReactNode } from 'react'

interface InputProps extends Omit<ComponentPropsWithRef<'input'>, 'size'> {
  label?: string
  error?: string
  helperText?: string
  size?: 'sm' | 'md' | 'lg' | 'xl'
  rightIcon?: ReactNode
}

export default function Input({
  label,
  error,
  helperText,
  size = 'md',
  rightIcon,
  className,
  id,
  name,
  ...props
}: InputProps) {
  const inputId = id || name

  return (
    <div className="space-y-2">
      {label && (
        <label htmlFor={inputId} className="block text-sm font-medium text-text-primary">
          {label}
        </label>
      )}

      <div className="relative">
        <input
          id={inputId}
          name={name}
          className={cn(
            'w-full rounded-lg',
            'bg-background border border-neutral-200',
            'text-text-primary',
            'placeholder:text-text-tertiary',
            'transition-all duration-200',
            'focus:outline-none focus:border-2 focus:border-primary-600',
            'disabled:bg-neutral-50 disabled:text-neutral-200 disabled:cursor-not-allowed disabled:border-neutral-600',
            error && 'border-danger-600 focus:border-2 focus:border-danger-600',

            size === 'sm' && 'h-8 px-3 text-sm placeholder:text-xs',
            size === 'md' && 'h-10 px-4 placeholder:text-sm',
            size === 'lg' && 'h-12 px-5 text-lg placeholder:text-base',
            size === 'xl' && 'h-16 px-6 text-xl placeholder:text-lg',

            rightIcon && size === 'sm' && 'pr-8',
            rightIcon && size === 'md' && 'pr-10',
            rightIcon && size === 'lg' && 'pr-12',
            rightIcon && size === 'xl' && 'pr-16',

            className
          )}
          aria-invalid={!!error}
          aria-describedby={
            error ? `${inputId}-error` : helperText ? `${inputId}-helper` : undefined
          }
          {...props}
        />

        {rightIcon && (
          <div
            className={cn(
              'absolute right-0 top-1/2 -translate-y-1/2 flex items-center justify-center text-text-secondary',
              size === 'sm' && 'w-8',
              size === 'md' && 'w-10',
              size === 'lg' && 'w-12',
              size === 'xl' && 'w-16'
            )}
          >
            {rightIcon}
          </div>
        )}
      </div>

      {error && (
        <p id={`${inputId}-error`} className="text-xs text-danger-600" role="alert">
          {error}
        </p>
      )}

      {!error && helperText && (
        <p id={`${inputId}-helper`} className="text-xs text-text-secondary">
          {helperText}
        </p>
      )}
    </div>
  )
}
