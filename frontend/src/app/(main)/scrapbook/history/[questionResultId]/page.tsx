import HistoryDetail from '@/components/scrapbook/HistoryDetail'
import { Metadata } from 'next'

export const metadata: Metadata = {
  title: '스크랩 문제 상세',
  description: '스크랩한 문제의 풀이와 AI 채팅 요약과 생성 변형문제를 확인하세요',
  robots: { index: false },
}

export default function ExamHistory() {
  return (
    <div className="fixed inset-0 top-16 flex flex-col">
      <HistoryDetail />
    </div>
  )
}
