import DashboardContainer from '@/components/dashboard/DashboardContainer'
import { Metadata } from 'next'

export const metadata: Metadata = {
  title: '대시보드',
  description: '나의 문제 풀이 현황과 학습 진행 상황을 확인하세요',
  robots: {
    index: true,
    follow: true,
  },
}

export default function HomePage() {
  return (
    <div className="min-h-[calc(100vh-4rem)] pt-10 sm:pt-0 flex items-center justify-center">
      <DashboardContainer />
    </div>
  )
}
