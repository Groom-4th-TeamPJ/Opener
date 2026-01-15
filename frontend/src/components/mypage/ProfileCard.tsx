import Button from '@/components/common/Button'
import { Card, CardContent } from '@/components/common/Card'

export default function ProfileCard() {
  return (
    <Card className="flex-1">
      <CardContent className="h-full sm:flex lg:flex-col justify-between lg:justify-center lg:gap-6 items-center">
        <div className="flex lg:flex-col justify-center items-center gap-6 lg:gap-4 text-start lg:text-center">
          <div className="size-20 rounded-full bg-neutral-200" />
          <div>
            <h1 className="font-bold text-xl">홍길동</h1>
            <span className="text-text-secondary">user@example.com</span>
          </div>
        </div>
        <Button
          variant="outline"
          className="w-full sm:w-fit border-neutral-100 text-text-primary hover:border-neutral-500 active:border-neutral-200 disabled:border-neutral-50 mt-4 sm:mt-0"
        >
          프로필 편집
        </Button>
      </CardContent>
    </Card>
  )
}
