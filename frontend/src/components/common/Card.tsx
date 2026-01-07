import { ComponentPropsWithRef, ReactNode } from 'react'
import { X } from 'lucide-react'
import cn from '@/utils/cn'

interface CardProps extends ComponentPropsWithRef<'div'> {
  children?: ReactNode
}

export function Card({ className, children, ...props }: CardProps) {
  return (
    <div
      className={cn('relative rounded-lg shadow-1 bg-white text-text-primary', className)}
      {...props}
    >
      {children}
    </div>
  )
}

interface CardHeaderProps extends ComponentPropsWithRef<'div'> {
  children?: ReactNode
  left?: ReactNode
  right?: ReactNode
  closable?: boolean
  onClose?: () => void
}

export function CardHeader({
  className,
  children,
  left,
  right,
  closable,
  onClose,
  ...props
}: CardHeaderProps) {
  return (
    <div
      className={cn('flex items-center justify-between p-6 text-text-primary', className)}
      {...props}
    >
      {left && <div className="mr-4">{left}</div>}
      <div className="flex-1 min-w-0 whitespace-pre-line wrap-break-word hyphens-auto">
        {children}
      </div>
      {(right || closable) && (
        <div className="ml-4 flex items-center gap-2">
          {right}
          {closable && (
            <button
              onClick={onClose}
              className="absolute top-3 right-3 p-2 rounded-lg hover:bg-foreground/5 transition-colors cursor-pointer z-10"
              aria-label="닫기"
            >
              <X className="size-4" />
            </button>
          )}
        </div>
      )}
    </div>
  )
}

interface CardContentProps extends ComponentPropsWithRef<'div'> {
  children?: ReactNode
}

export function CardContent({ className, children, ...props }: CardContentProps) {
  return (
    <div className={cn('p-6 text-text-primary', className)} {...props}>
      {children}
    </div>
  )
}

interface CardFooterProps extends ComponentPropsWithRef<'div'> {
  children?: ReactNode
}

export function CardFooter({ className, children, ...props }: CardFooterProps) {
  return (
    <div className={cn('flex items-center p-6 text-text-primary', className)} {...props}>
      {children}
    </div>
  )
}
