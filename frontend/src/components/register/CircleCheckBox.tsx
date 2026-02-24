import cn from '@/utils/cn'
import Image from 'next/image'

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

      <div
        className={cn(
          'w-5 h-5 rounded-full border flex items-center justify-center transition-all duration-200 shrink-0',
          checked
            ? 'bg-success-500 border-success-600'
            : 'bg-white border-neutral-300 group-hover:border-neutral-400'
        )}
      >
        {checked && (
          <Image
            src={'icons/check_white.svg'}
            alt="체크 아이콘"
            width={16}
            height={16}
            className=""
          />
        )}
      </div>

      <span className="text-neutral-600 text-sm font-medium">{label}</span>
    </label>
  )
}
