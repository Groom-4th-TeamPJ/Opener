import cn from '@/utils/cn'

const variantClasses = {
  correct: 'bg-success-200',
  wrong: 'bg-danger-200',
}

type FRQAnswer = {
  className?: string
  children: React.ReactNode
  variant: 'correct' | 'wrong'
}

export default function FRQAnswer({ className, children, variant }: FRQAnswer) {
  return (
    <div
      className={cn(
        'flex items-center p-3 rounded-lg  gap-1.5 md:max-w-137 md:h-12 md:text-lg lg:max-w-186 lg:h-16 lg:text-xl',
        className,
        variantClasses[variant]
      )}
    >
      {children}
    </div>
  )
}
