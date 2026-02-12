'use client'

import { memo } from 'react'
import cn from '@/utils/cn'
import type { ChatMessage } from '@/types/exam'
import AISparklesIcon from '@/components/icons/AISparklesIcon'
import StreamdownRenderer from './StreamdownRenderer'

interface ChatMessageItemProps {
  message: ChatMessage
}

function ChatMessageItem({ message }: ChatMessageItemProps) {
  const isUser = message.role === 'USER'

  return (
    <div
      className={cn('animate-fade-in flex flex-col gap-2', isUser ? 'items-end' : 'items-start')}
    >
      {message.role === 'ASSISTANT' ? (
        <div className="flex gap-2 w-full">
          <div className="shrink-0 w-8 h-8 bg-primary-50 rounded-full flex items-center justify-center">
            <AISparklesIcon className="w-5 h-5" />
          </div>
          <div className="flex flex-col gap-2 flex-1">
            <div className="max-w-64 min-w-44 px-3 py-2.5 bg-neutral-50 rounded-tr-lg rounded-bl-lg rounded-br-lg flex flex-col gap-1">
              <div className="text-text-primary text-sm whitespace-pre-wrap [&_.katex]:text-base [&_p]:m-0">
                <StreamdownRenderer content={message.content} />
              </div>
            </div>
            <span className="text-neutral-300 text-xs">{message.timestamp}</span>
          </div>
        </div>
      ) : (
        <div className="flex flex-col items-end gap-2">
          <div className="max-w-64 min-w-44 px-3 py-2.5 bg-primary-100 rounded-tl-lg rounded-bl-lg rounded-br-lg">
            <p className="text-text-primary text-sm whitespace-pre-wrap">{message.content}</p>
          </div>
          <span className="text-neutral-300 text-xs">{message.timestamp}</span>
        </div>
      )}
    </div>
  )
}

export default memo(ChatMessageItem)
