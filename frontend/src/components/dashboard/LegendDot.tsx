import cn from '@/utils/cn'

type LegendDotProps = {
  label: string
  className?: string
}

export default function LegendDot({ label, className }: LegendDotProps) {
  return (
    <span
      className={cn(
        "before:content-[''] before:inline-block before:size-2.5 before:rounded-full before:mr-1.5",
        'text-sm',
        className
      )}
    >
      {label}
    </span>
  )
}
