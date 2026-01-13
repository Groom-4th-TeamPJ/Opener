'use client'

import AIChatbot from '@/components/shared/AIChatbot'
import PromptAnalysisCard from './PromptAnalysisCard'
import ScrapbookQuestionCard from './ScrapbookQuestionCard'
import { mockScrapbookChatData } from '@/mocks/scrapbook-chat'
import type { ChatMessage } from '@/types/exam'

export default function HistoryDetail() {
  const { data } = mockScrapbookChatData

  // Mock data의 chat을 ChatMessage 형식으로 변환 (order를 id로 매핑)
  const chatMessages: ChatMessage[] = data.chat.map((msg) => ({
    id: msg.order,
    role: msg.role,
    content: msg.content,
    timestamp: msg.timestamp,
  }))

  return (
    <div className="h-dvh overflow-hidden bg-neutral-50 px-8 py-6">
      <div className="flex w-full h-full max-w-6xl mx-auto items-stretch gap-6">
        {/* Left: Question + Prompt Analysis */}
        <div className="flex-1 flex flex-col gap-6 h-full overflow-hidden">
          <div className="flex-1 overflow-y-auto min-h-0">
            <ScrapbookQuestionCard data={data} />
          </div>
          <div className="shrink-0">
            <PromptAnalysisCard />
          </div>
        </div>

        {/* Right: AI Chatbot */}
        <AIChatbot
          isActive={true}
          question={data}
          selectedChoice={data.select}
          frqAnswer=""
          initialMessages={chatMessages}
          placeholder="대화가 종료되었습니다."
          isDisabled={true}
        />
      </div>
    </div>
  )
}
