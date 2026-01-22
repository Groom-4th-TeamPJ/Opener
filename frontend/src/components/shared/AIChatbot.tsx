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
import { useSendChatMessage } from '@/hooks/exam/queries/use-send-chat-message'
import { useExamStore } from '@/stores/use-exam-store'

interface AIChatbotProps {
  isActive: boolean
  question: Question
  selectedChoice: number | null
  frqAnswer: string
  placeholder?: string
  isDisabled?: boolean
}

const INFO_TOOLTIP_TEXT =
  'AI 대화는 문제별로 진행됩니다.\n대화를 종료하거나 다음 문제로 이동하면 현재 대화는 종료되며, 대화 기록은 스크랩북에 자동 저장됩니다.'

const defaultQuestionState = {
  chatMessages: [] as ChatMessage[],
  streamingMessage: '',
}

export default function AIChatbot({
  isActive,
  question,
  selectedChoice,
  frqAnswer,
  placeholder = '질문을 입력하세요.',
  isDisabled = false,
}: AIChatbotProps) {
  const [inputValue, setInputValue] = useState('')
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const { mutate: sendChatMessage, isPending } = useSendChatMessage()
  const examResultId = useExamStore((state) => state.examResultId)
  const addChatMessage = useExamStore((state) => state.addChatMessage)

  // Store에서 메시지와 스트리밍 상태 가져오기
  const questionState = useExamStore((state) => state.questionStates[question.questionId])
  const { chatMessages, streamingMessage } = questionState ?? defaultQuestionState
  const isStreaming = !!streamingMessage

  // 문제가 변경되면 인풋 리셋
  useEffect(() => {
    setInputValue('')
  }, [question.questionId])

  // 분석 활성화 시 초기 AI 메시지 추가
  useEffect(() => {
    if (isActive && chatMessages.length === 0) {
      const userAnswer =
        question.questionType === 'MCQ' ? `${selectedChoice}번이` : `${frqAnswer}이/가`
      const contentText = `왜 ${userAnswer} 정답이라고 생각하셨나요?\n어떤 근거로 그렇게 판단하셨는지 설명해주세요!`

      addChatMessage(question.questionId, {
        id: 1,
        role: 'ASSISTANT',
        content: contentText,
        timestamp: formatChatTimestamp(new Date()),
      })
    }
  }, [
    isActive,
    chatMessages.length,
    question.questionType,
    question.questionId,
    selectedChoice,
    frqAnswer,
    addChatMessage,
  ])

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [chatMessages, streamingMessage])

  const handleSend = () => {
    if (!inputValue.trim() || !isActive || isDisabled || isPending || isStreaming || !examResultId)
      return

    const userMessage: ChatMessage = {
      id: chatMessages.length + 1,
      role: 'USER',
      content: inputValue,
      timestamp: formatChatTimestamp(new Date()),
    }

    addChatMessage(question.questionId, userMessage)

    sendChatMessage({
      sessionId: examResultId,
      questionId: question.questionId,
      message: inputValue,
    })

    // 한글 IME 조합 완료 후 인풋 비우기
    setTimeout(() => setInputValue(''), 0)
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      e.stopPropagation()
      handleSend()
    }
  }

  return (
    <div
      className={cn(
        'hidden sm:flex md:w-86 lg:w-96 h-full bg-white rounded-20 shadow-1 flex-col overflow-hidden',
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
            {chatMessages.map((message) => (
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

            {/* 스트리밍 중인 AI 메시지 */}
            {streamingMessage && (
              <div className="animate-fade-in flex flex-col gap-2 items-start">
                <div className="flex gap-2 w-full">
                  <div className="shrink-0 w-8 h-8 bg-primary-50 rounded-full flex items-center justify-center">
                    <AISparklesIcon className="w-5 h-5" />
                  </div>
                  <div className="flex flex-col gap-2 flex-1">
                    <div className="max-w-64 min-w-44 px-3 py-2.5 bg-neutral-50 rounded-tr-lg rounded-bl-lg rounded-br-lg flex flex-col gap-1">
                      <p className="text-text-primary text-sm whitespace-pre-wrap">
                        {streamingMessage}
                      </p>
                    </div>
                  </div>
                </div>
              </div>
            )}

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
          disabled={!isActive || isDisabled || isStreaming}
          size="lg"
          className="disabled:border-neutral-200"
          rightIcon={
            <Button
              onClick={handleSend}
              disabled={!isActive || isDisabled || isPending || isStreaming || !inputValue.trim()}
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
