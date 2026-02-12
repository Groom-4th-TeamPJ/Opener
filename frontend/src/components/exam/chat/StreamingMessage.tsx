'use client'

import { memo } from 'react'
import AISparklesIcon from '@/components/icons/AISparklesIcon'
import StreamdownRenderer from './StreamdownRenderer'
import { useExamStore } from '@/stores/use-exam-store'

interface StreamingMessageProps {
  questionId: number
}

function StreamingMessage({ questionId }: StreamingMessageProps) {
  // streamingMessages만 직접 구독 → 부모(AIChatbot)는 리렌더 안됨
  const streamingMessage = useExamStore((state) => state.streamingMessages[questionId] ?? '')

  if (!streamingMessage) return null

  return (
    <div className="animate-fade-in flex flex-col gap-2 items-start">
      <div className="flex gap-2 w-full">
        <div className="shrink-0 w-8 h-8 bg-primary-50 rounded-full flex items-center justify-center">
          <AISparklesIcon className="w-5 h-5" />
        </div>
        <div className="flex flex-col gap-2 flex-1">
          <div className="max-w-64 min-w-44 px-3 py-2.5 bg-neutral-50 rounded-tr-lg rounded-bl-lg rounded-br-lg flex flex-col gap-1">
            <StreamdownRenderer content={streamingMessage} />
          </div>
        </div>
      </div>
    </div>
  )
}

export default memo(StreamingMessage)
