import { Card, CardContent, CardFooter, CardHeader } from '@/components/common/Card'
import Image from 'next/image'
import Button from '@/components/common/Button'

export default function CustomerSupportCard() {
  return (
    <Card className="flex-1">
      <CardHeader
        left={
          <Image
            src={'icons/support-agent_primary.svg'}
            alt="FAQ"
            width={40}
            height={40}
            className="bg-primary-50 rounded-full p-2"
          />
        }
        className="text-lg font-bold pb-0"
      >
        고객 지원
      </CardHeader>
      <CardContent className="pt-2 pb-10">
        <span className="text-sm text-text-secondary">원하는 답을 찾지 못하셨나요?</span>
        <div className="mt-4 bg-neutral-50 rounded-lg py-3 px-4 text-sm text-text-secondary">
          궁금한 점이 있으시다면 언제든지 문의해주세요.
          <br />
          평일 9:00 ~ 18:00
        </div>
      </CardContent>
      <CardFooter>
        <Button
          variant="outline"
          widthFull
          size="lg"
          className="border-neutral-100 text-text-primary hover:border-neutral-500 active:border-neutral-200 disabled:border-neutral-50"
        >
          1:1 문의하기
        </Button>
      </CardFooter>
    </Card>
  )
}
