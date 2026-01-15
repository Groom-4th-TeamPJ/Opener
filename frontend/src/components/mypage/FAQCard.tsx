import { Card, CardContent, CardHeader } from '@/components/common/Card'
import Image from 'next/image'
import FAQItem from './FAQItem'

const FAQS = [
  { label: '구독 플랜 변경 방법' },
  { label: '결제 취소는 어떻게 하나요?' },
  { label: '계정 정보 수정하기' },
] as const

export default function FAQCard() {
  return (
    <Card className="flex-1">
      <CardHeader
        left={
          <Image
            src={'icons/question-line_primary.svg'}
            alt="FAQ"
            width={40}
            height={40}
            className="bg-primary-50 rounded-full p-2"
          />
        }
        right={<span className="text-sm text-text-secondary font-normal">더보기</span>}
        className="text-lg font-bold pb-0"
      >
        FAQ
      </CardHeader>
      <CardContent className="pt-2">
        <span className="text-sm text-text-secondary">자주 묻는 질문을 확인하세요.</span>
        <div className="mt-4 space-y-4">
          {FAQS.map((item) => (
            <FAQItem key={item.label} label={item.label} />
          ))}
        </div>
      </CardContent>
    </Card>
  )
}
