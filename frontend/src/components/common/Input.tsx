import cn from '@/utils/cn'
import { ComponentPropsWithRef } from 'react'

interface InputProps extends ComponentPropsWithRef<'input'> {
  label?: string
  error?: string
  helperText?: string
}

export default function Input({
  label,
  error,
  helperText,
  className,
  id,
  name,
  ...props
}: InputProps) {
  const inputId = id || name

  return (
    <div className="space-y-2">
      {label && (
        <label htmlFor={inputId} className="block text-sm font-medium text-foreground">
          {label}
        </label>
      )}

      <input
        id={inputId}
        name={name}
        className={cn(
          'w-full px-4 py-2 rounded-xl',
          'bg-background border border-foreground/20',
          'text-foreground text-base',
          'placeholder:text-foreground/40 placeholder:text-sm',
          'transition-all duration-200',
          'focus:outline-none focus:border-primary-600',
          error && 'border-danger-600 focus:border-danger-600',
          className
        )}
        aria-invalid={!!error}
        aria-describedby={error ? `${inputId}-error` : helperText ? `${inputId}-helper` : undefined}
        {...props}
      />

      {error && (
        <p id={`${inputId}-error`} className="text-xs text-danger-600" role="alert">
          {error}
        </p>
      )}

      {!error && helperText && (
        <p id={`${inputId}-helper`} className="text-xs text-foreground/60">
          {helperText}
        </p>
      )}
    </div>
  )
}
