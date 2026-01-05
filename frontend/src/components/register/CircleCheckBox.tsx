import { Check } from 'lucide-react'
import cn from '@/utils/cn'

interface CircleCheckboxProps {
  id?: string
  checked: boolean
  onChange: (checked: boolean) => void
  label: React.ReactNode
  className?: string
}

export default function CircleCheckbox({
  checked,
  onChange,
  label,
  className,
}: CircleCheckboxProps) {
  return (
    <label
      className={cn('flex items-center gap-2 cursor-pointer group', className)}
      onClick={(e) => e.stopPropagation()} // 이벤트 버블링 방지
    >
      <input
        type="checkbox"
        className="sr-only"
        checked={checked}
        onChange={(e) => onChange(e.target.checked)}
      />

      {/* 커스텀 원형 UI */}
      <div
        className={cn(
          'w-5 h-5 rounded-full border flex items-center justify-center transition-all duration-200 shrink-0',
          checked
            ? 'bg-success-500 border-success-600'
            : 'bg-white border-neutral-300 group-hover:border-neutral-400'
        )}
      >
        {checked && <Check className="w-3 h-3 text-white" strokeWidth={4} />}
      </div>

      {/* 라벨 영역 */}
      <span className="text-neutral-600 text-sm font-medium">{label}</span>
    </label>
  )
}
