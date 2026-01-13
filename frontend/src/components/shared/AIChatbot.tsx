'use client'

import { useState, useRef, useEffect } from 'react'
import { Send } from 'lucide-react'
import cn from '@/utils/cn'
import type { Question, ChatMessage } from '@/types/exam'
import Input from '@/components/common/Input'
import Button from '@/components/common/Button'
import { formatChatTimestamp } from '@/utils/format'
import AISparklesIcon from '@/components/icons/AISparklesIcon'
import { InfoTooltip } from '@/components/common/InfoTooltip'

interface AIChatbotProps {
  isActive: boolean
  question: Question
  selectedChoice: number | null
  frqAnswer: string
  initialMessages?: ChatMessage[]
  placeholder?: string
  isDisabled?: boolean
}

const INFO_TOOLTIP_TEXT =
  'AI 대화는 문제별로 진행됩니다.\n대화를 종료하거나 다음 문제로 이동하면 현재 대화는 종료되며, 대화 기록은 스크랩북에 자동 저장됩니다.'

export default function AIChatbot({
  isActive,
  question,
  selectedChoice,
  frqAnswer,
  initialMessages,
  placeholder = '질문을 입력하세요.',
  isDisabled = false,
}: AIChatbotProps) {
  const [messages, setMessages] = useState<ChatMessage[]>(initialMessages || [])
  const [inputValue, setInputValue] = useState('')
  const messagesEndRef = useRef<HTMLDivElement>(null)

  // 문제가 변경되면 채팅 내용 리셋
  useEffect(() => {
    if (!initialMessages) {
      setMessages([])
      setInputValue('')
    }
  }, [question.questionId, initialMessages])

  useEffect(() => {
    if (isActive && messages.length === 0 && !initialMessages) {
      const userAnswer = question.type === 'MCQ' ? `${selectedChoice}번` : frqAnswer
      const highlightText = `${userAnswer}을 선택했네요.`
      const contentText = `왜 ${userAnswer}이 정답이라고 생각하셨나요?\n어떤 근거로 그렇게 판단하셨는지 설명해주세요!`

      setMessages([
        {
          id: 1,
          role: 'ASSISTANT',
          content: contentText,
          timestamp: formatChatTimestamp(new Date()),
          highlight: highlightText,
        },
      ])
    }
  }, [isActive, messages.length, question.type, selectedChoice, frqAnswer, initialMessages])

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const handleSend = () => {
    if (!inputValue.trim() || !isActive || isDisabled) return

    const userMessage: ChatMessage = {
      id: messages.length + 1,
      role: 'USER',
      content: inputValue,
      timestamp: formatChatTimestamp(new Date()),
    }

    setMessages((prev) => [...prev, userMessage])
    setInputValue('')

    setTimeout(() => {
      const assistantMessage: ChatMessage = {
        id: messages.length + 2,
        role: 'ASSISTANT',
        content:
          '네, 좋은 질문이에요! 미분 공식 중 다항함수의 미분법을 적용하면 됩니다. xⁿ을 미분하면 n·xⁿ⁻¹이 되는 것을 기억하세요.',
        timestamp: formatChatTimestamp(new Date()),
      }
      setMessages((prev) => [...prev, assistantMessage])
    }, 1000)
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  return (
    <div
      className={cn(
        'hidden sm:flex md:w-86 lg:w-96 bg-white rounded-20 shadow-1 flex-col overflow-hidden',
        !isActive && 'opacity-30'
      )}
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
        {!isActive ? (
          <div />
        ) : (
          <div className="space-y-4">
            {messages.map((message) => (
              <div
                key={message.id}
                className={cn(
                  'animate-fade-in flex flex-col gap-2',
                  message.role === 'USER' ? 'items-end' : 'items-start'
                )}
              >
                {message.role === 'ASSISTANT' ? (
                  <div className="flex gap-2 w-full">
                    <div className="shrink-0 w-8 h-8 bg-primary-50 rounded-full flex items-center justify-center">
                      <AISparklesIcon className="w-5 h-5" />
                    </div>
                    <div className="flex flex-col gap-2 flex-1">
                      <div className="max-w-64 min-w-44 px-3 py-2.5 bg-neutral-50 rounded-tr-lg rounded-bl-lg rounded-br-lg flex flex-col gap-1">
                        {message.highlight && (
                          <p className="text-primary-600 text-sm font-medium whitespace-pre-line">
                            {message.highlight}
                          </p>
                        )}
                        <p className="text-text-primary text-sm whitespace-pre-wrap">
                          {message.content}
                        </p>
                      </div>
                      <span className="text-neutral-300 text-xs">{message.timestamp}</span>
                    </div>
                  </div>
                ) : (
                  <div className="flex flex-col items-end gap-2">
                    <div className="max-w-64 min-w-44 px-3 py-2.5 bg-primary-100 rounded-tl-lg rounded-bl-lg rounded-br-lg">
                      <p className="text-text-primary text-sm whitespace-pre-wrap">
                        {message.content}
                      </p>
                    </div>
                    <span className="text-neutral-300 text-xs">{message.timestamp}</span>
                  </div>
                )}
              </div>
            ))}
            <div ref={messagesEndRef} />
          </div>
        )}
      </div>

      {/* Input Area */}
      <div className="p-4 border-t border-neutral-100 shrink-0">
        <Input
          type="text"
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={placeholder}
          disabled={!isActive || isDisabled}
          size="lg"
          className="disabled:border-neutral-200"
          rightIcon={
            <Button
              onClick={handleSend}
              disabled={!isActive || isDisabled || !inputValue.trim()}
              variant="ghost"
              className="p-0 h-auto min-h-0 text-primary-600 hover:bg-transparent hover:opacity-80"
            >
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
