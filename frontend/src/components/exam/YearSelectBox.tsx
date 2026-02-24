'use client'

import { useState, useEffect, useRef, useCallback, memo } from 'react'
import { ChevronDown } from 'lucide-react'
import Button from '@/components/common/Button'
import cn from '@/utils/cn'

interface YearSelectBoxProps {
  value: number | null
  onChange: (value: number) => void
  isSelected?: boolean
}

const currentYear = new Date().getFullYear()
const years = Array.from({ length: 4 }, (_, i) => currentYear - i)

export default memo(function YearSelectBox({
  value,
  onChange,
  isSelected = false,
}: YearSelectBoxProps) {
  const [isOpen, setIsOpen] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)

  // 외부 클릭 감지
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

  const handleSelect = useCallback(
    (year: number) => {
      onChange(year)
      setIsOpen(false)
    },
    [onChange]
  )

  return (
    <div className="relative" ref={containerRef}>
      {/* Trigger Button */}
      <div className="relative">
        <Button
          variant="ghost"
          onClick={() => setIsOpen(!isOpen)}
          className={cn(
            'w-full h-15 lg:h-20 bg-white px-4 rounded-lg justify-center',
            isSelected
              ? 'font-bold border-2 border-primary-600 hover:bg-white'
              : 'font-medium hover:bg-neutral-100'
          )}
        >
          <span
            className={cn(
              'text-center leading-7',
              isSelected ? 'text-text-primary font-bold' : 'text-text-secondary font-medium'
            )}
          >
            {value ?? '연도 선택'}
          </span>
        </Button>
        <ChevronDown
          className={cn(
            'absolute right-4 top-1/2 -translate-y-1/2 w-5 h-5 text-neutral-400 transition-transform duration-300 pointer-events-none',
            isOpen && 'rotate-180'
          )}
        />
      </div>

      {/* Dropdown */}
      {isOpen && (
        <div className="absolute top-full mt-4 w-full bg-white rounded-lg shadow-[0px_4px_30px_0px_rgba(40,42,46,0.08)] overflow-hidden z-50">
          {years.map((year, index) => (
            <Button
              key={year}
              variant="ghost"
              onClick={() => handleSelect(year)}
              className={cn(
                'w-full h-15 lg:h-20 px-4 relative justify-center rounded-none hover:bg-neutral-100',
                index < years.length - 1 && 'border-b border-neutral-200'
              )}
            >
              <span className="text-center text-text-secondary font-medium leading-7">{year}</span>
            </Button>
          ))}
        </div>
      )}
    </div>
  )
})
