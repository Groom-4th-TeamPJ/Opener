import { Card, CardContent, CardHeader } from '@/components/common/Card'
import Image from 'next/image'

type AccountInfoItemCardProps = {
  title: string
  description: string
  iconSrc: string
  iconAlt: string
}

export default function AccountInfoItemCard({
  title,
  description,
  iconSrc,
  iconAlt,
}: AccountInfoItemCardProps) {
  return (
    <Card className="shadow-none bg-neutral-50">
      <CardHeader
        left={<Image src={iconSrc} alt={iconAlt} width={24} height={24} />}
        className="pb-0"
      />
      <CardContent>
        <h1 className="font-bold">{title}</h1>
        <span className="text-sm text-text-secondary">{description}</span>
      </CardContent>
    </Card>
  )
}
