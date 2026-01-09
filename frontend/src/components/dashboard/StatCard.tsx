import Image from 'next/image'
import { Card, CardContent, CardHeader } from '@/components/common/Card'
import cn from '@/utils/cn'

type StatCardProps = {
  title: string
  iconSrc: string
  iconAlt: string
  value: string
  subText: string
  iconClassName?: string
}

export default function StatCard({
  title,
  iconSrc,
  iconAlt,
  value,
  subText,
  iconClassName,
}: StatCardProps) {
  return (
    <Card>
      <CardHeader
        className=" font-bold"
        left={
          <Image
            src={iconSrc}
            alt={iconAlt}
            width={30}
            height={30}
            className={cn('bg-primary-50 p-1.5 rounded-full', iconClassName)}
          />
        }
      >
        {title}
      </CardHeader>

      <CardContent className="space-y-1">
        <h1 className="text-lg lg:text-xl text-text-primary font-bold leading-tight">{value}</h1>
        <span className="text-xs lg:text-sm text-text-secondary">{subText}</span>
      </CardContent>
    </Card>
  )
}
