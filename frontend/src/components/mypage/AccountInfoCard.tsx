import { Card, CardContent, CardHeader } from '@/components/common/Card'
import Image from 'next/image'
import AccountInfoItemCard from './AccountInfoCardItem'

const ITEMS = [
  {
    title: '계정',
    description: '로그인 정보 관리',
    iconSrc: 'icons/account_gray.svg',
    iconAlt: '계정 관리',
  },
  {
    title: '구독 관리',
    description: '플랜 변경 및 결제',
    iconSrc: 'icons/payment_gray.svg',
    iconAlt: '구독 관리',
  },
  {
    title: '캔 이용내역',
    description: '재화 사용 기록',
    iconSrc: 'icons/details-of-use_gray.svg',
    iconAlt: '캔 이용내역',
  },
] as const

export default function AccountInfoCard() {
  return (
    <Card>
      <CardHeader
        left={
          <Image
            src={'icons/user_primary.svg'}
            alt="계정 정보"
            width={40}
            height={40}
            className="bg-primary-50 rounded-full p-2"
          />
        }
        className="text-lg font-bold pb-0"
      >
        계정 정보
      </CardHeader>
      <CardContent className="pt-2">
        <span className="text-sm text-text-secondary">개인정보 및 보안 설정을 관리하세요.</span>
        <div className="sm:grid grid-rows-1 grid-cols-3 gap-4 mt-4 space-y-4 sm:space-y-0">
          {ITEMS.map((item) => (
            <AccountInfoItemCard key={item.title} {...item} />
          ))}
        </div>
      </CardContent>
    </Card>
  )
}
