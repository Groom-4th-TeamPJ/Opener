import cn from '@/utils/cn'

const variantClasses = {
  default: 'bg-neutral-50',
  correct: 'bg-success-200',
  wrong: 'bg-danger-200',
}

type FRQAnswer = {
  className?: string
  children?: React.ReactNode
  variant: 'default' | 'correct' | 'wrong'
}

export default function FRQAnswer({ className, children, variant, ...props }: FRQAnswer) {
  return (
    <div
      className={cn(
        'flex items-center p-3 rounded-lg h-10 gap-1.5',
        variantClasses[variant],
        className
      )}
      {...props}
    >
      {children}
    </div>
  )
}
