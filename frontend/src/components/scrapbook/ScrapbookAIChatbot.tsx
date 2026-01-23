'use client'

import { useRef } from 'react'
import { Send } from 'lucide-react'
import cn from '@/utils/cn'
import type { ScrapbookQuestion } from '@/types/exam'
import Input from '@/components/common/Input'
import Button from '@/components/common/Button'
import AISparklesIcon from '@/components/icons/AISparklesIcon'
import { InfoTooltip } from '@/components/common/InfoTooltip'
import renderLatex from '@/utils/render-latex'

interface AIChatbotProps {
  isActive: boolean
  question: ScrapbookQuestion
  isDisabled?: boolean
}

const INFO_TOOLTIP_TEXT =
  'AI 대화는 문제별로 진행됩니다.\n대화를 종료하거나 다음 문제로 이동하면 현재 대화는 종료되며, 대화 기록은 스크랩북에 자동 저장됩니다.'

export default function ScrapbookAIChatbot({
  isActive,
  question,
  isDisabled = false,
}: AIChatbotProps) {
  const messagesEndRef = useRef<HTMLDivElement>(null)

  return (
    <div
      className={
        'hidden sm:flex md:w-86 lg:w-96 bg-white rounded-20 shadow-1 flex-col overflow-hidden'
      }
    >
      {/* Header */}
      <div className="px-4 py-5 border-b border-neutral-100 flex items-center gap-2.5 shrink-0">
        <div className="flex-1 flex items-center gap-2">
          <div className="w-2 h-2 bg-primary-600 rounded-full" />
          <h3 className="text-text-primary font-bold">AI 튜터</h3>
        </div>
        <InfoTooltip content={INFO_TOOLTIP_TEXT} side="bottom" disabled={!isActive}></InfoTooltip>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto scrollbar-overlay min-h-0 px-6 pt-6 pb-6">
        <div className="space-y-4">
          {question.chat.map((message) => (
            <div
              key={message.order}
              className={cn(
                'animate-fade-in flex flex-col gap-2',
                message.role === 'USER' ? 'items-end' : 'items-start'
              )}
            >
              {message.role === 'LLM' ? (
                <div className="flex gap-2 w-full">
                  <div className="shrink-0 w-8 h-8 bg-primary-50 rounded-full flex items-center justify-center">
                    <AISparklesIcon className="w-5 h-5" />
                  </div>
                  <div className="flex flex-col gap-2 flex-1">
                    <div className="max-w-64 min-w-44 px-3 py-2.5 bg-neutral-50 rounded-tr-lg rounded-bl-lg rounded-br-lg flex flex-col gap-1">
                      <p
                        className="text-text-primary text-sm whitespace-pre-wrap break-all"
                        dangerouslySetInnerHTML={{
                          __html: renderLatex(message.content, { blockDisplayMode: false }),
                        }}
                      />
                    </div>
                    <span className="text-neutral-300 text-xs">{message.timestamp}</span>
                  </div>
                </div>
              ) : (
                <div className="flex flex-col items-end gap-2">
                  <div className="max-w-64 min-w-44 px-3 py-2.5 bg-primary-100 rounded-tl-lg rounded-bl-lg rounded-br-lg">
                    <div
                      className="text-text-primary text-sm whitespace-pre-wrap break-all"
                      dangerouslySetInnerHTML={{
                        __html: renderLatex(message.content, { blockDisplayMode: false }),
                      }}
                    />
                  </div>
                  <span className="text-neutral-300 text-xs">{message.timestamp}</span>
                </div>
              )}
            </div>
          ))}
          <div ref={messagesEndRef} />
        </div>
      </div>

      {/* Input Area */}
      <div className="p-4 border-t border-neutral-100 shrink-0">
        <Input
          type="text"
          placeholder="대화가 종료되었습니다."
          disabled={isDisabled}
          size="lg"
          className="disabled:border-neutral-200"
          rightIcon={
            <Button disabled={isDisabled} variant="ghost" className="p-0 h-auto min-h-0 ">
              <Send className="w-5 h-5" />
            </Button>
          }
        />

        {/* Disclaimer */}
        <p className="text-neutral-200 text-xs text-center mt-2.5">
          AI 답변은 오류가 있을 수 있으니 교차 검증을 권장합니다.
        </p>
      </div>
    </div>
  )
}
