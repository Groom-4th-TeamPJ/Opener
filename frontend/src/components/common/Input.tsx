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
          'w-full px-4 py-2 rounded-2xl',
          'bg-background border border-input',
          'text-foreground text-base',
          'placeholder:text-muted-foreground',
          'transition-all duration-200',
          'focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-primary',
          error && 'border-destructive focus:ring-destructive/50 focus:border-destructive',
          className
        )}
        aria-invalid={!!error}
        aria-describedby={error ? `${inputId}-error` : helperText ? `${inputId}-helper` : undefined}
        {...props}
      />

      {error && (
        <p id={`${inputId}-error`} className="text-sm text-destructive" role="alert">
          {error}
        </p>
      )}

      {!error && helperText && (
        <p id={`${inputId}-helper`} className="text-sm text-muted-foreground">
          {helperText}
        </p>
      )}
    </div>
  )
}
