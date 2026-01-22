'use client'

import PromptAnalysisCard from './PromptAnalysisCard'
import ScrapbookQuestionCard from './ScrapbookQuestionCard'
import useScrapBookHistoryDetail from '@/hooks/scrapbook/use-scrapbook-history-detail'
import { useParams } from 'next/navigation'
import ScrapbookAIChatbot from './ScrapbookAIChatbot'

export default function HistoryDetail() {
  const param = useParams<{ questionResultId: string }>()
  const questionResultId = Number(param?.questionResultId)

  const {
    data: historyDetailData,
    isLoading,
    isError,
  } = useScrapBookHistoryDetail(questionResultId)

  if (isLoading) {
    return (
      <div className="h-dvh flex items-center justify-center bg-neutral-50">
        <p className="text-text-secondary">데이터를 불러오는 중...</p>
      </div>
    )
  }
  if (isError) {
    throw new Error()
  }
  if (!historyDetailData) {
    return (
      <div className="h-dvh flex items-center justify-center bg-neutral-50">
        <p className="text-text-secondary">데이터가 없습니다.</p>
      </div>
    )
  }

  return (
    <div className="h-dvh overflow-hidden bg-neutral-50 px-8 py-6">
      <div className="flex w-full h-full max-w-6xl mx-auto items-stretch gap-6">
        {/* Left: Question + Prompt Analysis */}
        <div className="flex-1 flex flex-col gap-6 h-full overflow-hidden">
          <div className="flex-1 overflow-y-auto min-h-0">
            <ScrapbookQuestionCard data={historyDetailData} />
          </div>
          <div className="shrink-0">
            <PromptAnalysisCard data={historyDetailData.promptSummary} />
          </div>
        </div>

        {/* Right: AI Chatbot */}
        <ScrapbookAIChatbot isActive={true} question={historyDetailData} isDisabled={true} />
      </div>
    </div>
  )
}
