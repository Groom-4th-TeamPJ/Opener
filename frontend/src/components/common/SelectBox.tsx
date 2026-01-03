'use client'

import cn from '@/utils/cn'
import { ComponentPropsWithRef, useEffect, useRef, useState } from 'react'
import { ChevronDown, Check } from 'lucide-react'

export interface SelectOption {
  value: string
  label: string
}

interface SelectBoxProps extends Omit<ComponentPropsWithRef<'div'>, 'onChange'> {
  label?: string
  error?: string
  helperText?: string
  placeholder?: string
  options: SelectOption[]
  value?: string
  onChange?: (value: string) => void
  disabled?: boolean
  triggerClassName?: string
}

export default function SelectBox({
  label,
  error,
  helperText,
  placeholder = '선택하세요',
  options,
  value,
  onChange,
  disabled = false,
  triggerClassName,
  className,
  ...props
}: SelectBoxProps) {
  const [isOpen, setIsOpen] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)
  const triggerRef = useRef<HTMLButtonElement>(null)
  const selectedOptionRef = useRef<HTMLButtonElement>(null)

  const selectedOption = options.find((opt) => opt.value === value)
  const displayText = selectedOption ? selectedOption.label : placeholder

  // 외부 클릭 및 ESC 키 처리
  useEffect(() => {
    if (!isOpen) return

    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false)
      }
    }

    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setIsOpen(false)
      }
    }

    document.addEventListener('mousedown', handleClickOutside)
    document.addEventListener('keydown', handleEscape)

    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
      document.removeEventListener('keydown', handleEscape)
    }
  }, [isOpen])

  // 선택된 옵션으로 자동 스크롤
  useEffect(() => {
    if (isOpen && selectedOptionRef.current) {
      selectedOptionRef.current.scrollIntoView({
        block: 'nearest',
        behavior: 'smooth',
      })
    }
  }, [isOpen])

  const handleSelect = (optionValue: string) => {
    onChange?.(optionValue)
    setIsOpen(false)
  }

  return (
    <div className={cn('space-y-2', className)} ref={containerRef} {...props}>
      {label && <label className="block text-sm font-medium text-text-primary">{label}</label>}

      <div className="relative">
        {/* Trigger Button */}
        <button
          ref={triggerRef}
          type="button"
          onClick={() => !disabled && setIsOpen(!isOpen)}
          disabled={disabled}
          className={cn(
            'w-full px-4 py-2 rounded-xl',
            'bg-background border border-neutral-200',
            'text-text-primary text-sm text-left',
            'transition-all duration-200',
            'flex items-center justify-between gap-2',
            error && 'border-danger-600 focus:border-danger-600',
            disabled && 'opacity-50 cursor-not-allowed',
            !disabled && 'cursor-pointer hover:border-foreground/30',
            triggerClassName
          )}
        >
          <span className={cn(!selectedOption && 'text-foreground/20 text-sm')}>{displayText}</span>
          <ChevronDown
            className={cn(
              'w-4 h-4 text-foreground/50 transition-transform duration-300',
              isOpen && 'rotate-180'
            )}
          />
        </button>

        {/* Dropdown Menu */}
        {isOpen && (
          <div
            className={cn(
              'absolute z-50 mt-1 w-full',
              'bg-background border border-foreground/20 rounded-xl shadow-lg',
              'max-h-60 overflow-auto',
              'p-1'
            )}
          >
            {options.map((option) => (
              <button
                key={option.value}
                ref={option.value === value ? selectedOptionRef : null}
                type="button"
                onClick={() => handleSelect(option.value)}
                className={cn(
                  'w-full px-4 py-2 text-left text-sm rounded-lg',
                  'transition-colors duration-150',
                  'flex items-center justify-between',
                  'hover:bg-foreground/5',
                  option.value !== value && 'text-foreground'
                )}
              >
                <span>{option.label}</span>
                {option.value === value && <Check className="w-4 h-4 text-primary-600 " />}
              </button>
            ))}
          </div>
        )}
      </div>

      {error && (
        <p className="text-sm text-danger-600" role="alert">
          {error}
        </p>
      )}

      {!error && helperText && <p className="text-sm text-foreground/60">{helperText}</p>}
    </div>
  )
}
