import AccountInfoCard from '@/components/mypage/AccountInfoCard'
import CustomerSupportCard from '@/components/mypage/CustomerSupportCard'
import FAQCard from '@/components/mypage/FAQCard'
import ProfileCard from '@/components/mypage/ProfileCard'
import { Metadata } from 'next'

export const metadata: Metadata = {
  title: '마이페이지',
  description: '내 정보와 학습 설정을 관리하세요',
}

export default function MyPage() {
  return (
    <div className="min-h-[calc(100vh-4rem)] pt-10 sm:pt-0 flex items-center justify-center">
      <main className="w-full max-w-6xl px-4 mx-auto">
        <h1 className="text-[1.75rem] font-bold text-text-primary mb-10">마이페이지</h1>
        <section className="lg:flex justify-center gap-4 space-y-4 lg:space-y-0">
          <ProfileCard />
          <div className="flex-2 space-y-4">
            <AccountInfoCard />
            <div className="sm:flex gap-4 space-y-4 sm:space-y-0">
              <FAQCard />
              <CustomerSupportCard />
            </div>
          </div>
        </section>
      </main>
    </div>
  )
}
