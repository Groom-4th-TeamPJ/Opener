import Image from 'next/image'
import Link from 'next/link'
import { Card, CardContent } from '@/components/common/Card'
import cn from '@/utils/cn'
import { ROUTES } from '@/constants/routes'
import { ReactNode } from 'react'

type StartSolveCardProps = {
  title: ReactNode
  description: ReactNode
  hasStats?: boolean
  label: string
  disabled?: boolean
}

export default function StartSolveCard({
  hasStats = false,
  title,
  description,
  label,
  disabled = false,
}: StartSolveCardProps) {
  return (
    <Card
      className={cn(
        'flex flex-col items-center justify-center flex-1 lg:py-8',
        !hasStats && 'min-h-[70vh]'
      )}
    >
      <CardContent className="text-center space-y-6 mx-auto">
        <div className="relative size-15 lg:size-20 mx-auto">
          <Image src="icons/writing_gray.svg" alt="필기 아이콘" fill />
        </div>

        <div className="lg:space-y-2">
          <h1 className="text-lg lg:text-xl text-text-primary font-bold leading-tight">{title}</h1>
          <span className="text-xs lg:text-sm text-text-secondary">{description}</span>
        </div>

        <Link
          href={ROUTES.EXAM}
          aria-disabled={disabled}
          className={cn(
            'inline-flex items-center justify-center gap-2 rounded-lg cursor-pointer',
            'font-bold h-10 lg:h-12 px-6 text-base lg:w-full',
            'bg-primary-600 text-background hover:bg-primary-500 active:bg-primary-700',
            disabled && 'bg-primary-800 pointer-events-none'
          )}
        >
          {label}
        </Link>
      </CardContent>
    </Card>
  )
}
