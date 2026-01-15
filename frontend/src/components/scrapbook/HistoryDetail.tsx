'use client'

import { useState } from 'react'
import AIChatbot from '@/components/shared/AIChatbot'
import PromptAnalysisCard from './PromptAnalysisCard'
import ScrapbookQuestionCard from './ScrapbookQuestionCard'
import type { ScrapbookQuestion, ChatMessage } from '@/types/exam'

export default function HistoryDetail() {
  // TODO: API 연동 필요
  const [data] = useState<ScrapbookQuestion | null>(null)

  if (!data) {
    return (
      <div className="h-dvh flex items-center justify-center bg-neutral-50">
        <p className="text-text-secondary">데이터를 불러오는 중...</p>
      </div>
    )
  }

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
