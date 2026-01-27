import cn from '@/utils/cn'

interface LoadingProps {
  dotClassName?: string
  className?: string
}

export default function Loading({ dotClassName = 'bg-neutral-100', className }: LoadingProps) {
  return (
    <div className={cn('inline-flex justify-start items-center gap-1', className)}>
      <div className={cn('w-2 h-2 rounded-full animate-pulse-wave', dotClassName)} />
      <div
        className={cn('w-2 h-2 rounded-full animate-pulse-wave animation-delay-200', dotClassName)}
      />
      <div
        className={cn('w-2 h-2 rounded-full animate-pulse-wave animation-delay-400', dotClassName)}
      />
    </div>
  )
}
