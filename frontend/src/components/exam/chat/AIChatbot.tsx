'use client'

import { useState, useRef, useEffect } from 'react'
import { Send } from 'lucide-react'
import { useShallow } from 'zustand/react/shallow'
import cn from '@/utils/cn'
import type { Question, ChatMessage } from '@/types/exam'
import Input from '@/components/common/Input'
import Button from '@/components/common/Button'
import { formatChatTimestamp } from '@/utils/format'
import AISparklesIcon from '@/components/icons/AISparklesIcon'
import { InfoTooltip } from '@/components/common/InfoTooltip'
import { useSendChatMessage } from '@/hooks/exam/queries/use-send-chat-message'
import { useExamStore } from '@/stores/use-exam-store'
import Loading from '@/components/shared/Loading'
import ChatMessageItem from './ChatMessageItem'
import StreamingMessage from './StreamingMessage'

interface AIChatbotProps {
  isActive: boolean
  question: Question
  placeholder?: string
  isDisabled?: boolean
}

const INFO_TOOLTIP_TEXT =
  'AI 대화는 문제별로 진행됩니다.\n대화를 종료하거나 다음 문제로 이동하면 현재 대화는 종료되며, 대화 기록은 스크랩북에 자동 저장됩니다.'

const EMPTY_CHAT_MESSAGES: ChatMessage[] = []

export default function AIChatbot({
  isActive,
  question,
  placeholder = '질문을 입력하세요.',
  isDisabled = false,
}: AIChatbotProps) {
  const [inputValue, setInputValue] = useState('')
  const [isWaitingResponse, setIsWaitingResponse] = useState(false)
  const [showDelayMessage, setShowDelayMessage] = useState(false)
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLInputElement>(null)
  const prevIsStreamingRef = useRef(false)
  const { mutate: sendChatMessage } = useSendChatMessage()
  const examResultId = useExamStore((state) => state.examResultId)
  const addChatMessage = useExamStore((state) => state.addChatMessage)
  const { setStreamingMessage } = useExamStore.getState()

  const { chatMessages, isStreaming } = useExamStore(
    useShallow((state) => ({
      chatMessages: state.questionStates[question.questionId]?.chatMessages ?? EMPTY_CHAT_MESSAGES,
      isStreaming: !!state.streamingMessages[question.questionId],
    }))
  )

  // 문제가 변경되면 상태 리셋
  useEffect(() => {
    setInputValue('')
    setIsWaitingResponse(false)
  }, [question.questionId])

  // 분석 활성화 시 로딩 표시
  useEffect(() => {
    if (isActive && chatMessages.length === 0 && !isStreaming) {
      setIsWaitingResponse(true)
    }
  }, [isActive, chatMessages.length, isStreaming])

  // 스트리밍이 시작되면 대기 상태 해제
  useEffect(() => {
    if (isStreaming) {
      setIsWaitingResponse(false)
    }
  }, [isStreaming])

  // 15초 이상 대기 시 안내 메시지 표시
  useEffect(() => {
    if (!isWaitingResponse) {
      setShowDelayMessage(false)
      return
    }

    const timer = setTimeout(() => {
      setShowDelayMessage(true)
    }, 15000)

    return () => clearTimeout(timer)
  }, [isWaitingResponse])

  // AI 응답이 끝나면 입력창에 포커스
  useEffect(() => {
    const wasStreaming = prevIsStreamingRef.current
    const isNowNotStreaming = !isStreaming

    if (wasStreaming && isNowNotStreaming && isActive) {
      inputRef.current?.focus()
    }
    prevIsStreamingRef.current = isStreaming
  }, [isStreaming, isActive])

  useEffect(() => {
    requestAnimationFrame(() => {
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
    })
  }, [chatMessages, isStreaming, isWaitingResponse])

  const handleSend = () => {
    if (
      !inputValue.trim() ||
      !isActive ||
      isDisabled ||
      isWaitingResponse ||
      isStreaming ||
      !examResultId
    )
      return

    setIsWaitingResponse(true)
    setStreamingMessage(question.questionId, '')

    const userMessage: ChatMessage = {
      id: chatMessages.length + 1,
      role: 'USER',
      content: inputValue,
      timestamp: formatChatTimestamp(new Date()),
    }

    addChatMessage(question.questionId, userMessage)

    sendChatMessage(
      {
        sessionId: examResultId,
        questionId: question.questionId,
        message: inputValue,
      },
      {
        onError: () => setIsWaitingResponse(false),
      }
    )

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
              <ChatMessageItem key={message.id} message={message} />
            ))}

            {/* AI 응답 대기 중 로딩 */}
            {isWaitingResponse && (
              <div className="animate-fade-in flex flex-col gap-2 items-start">
                <div className="flex gap-2 w-full">
                  <div className="shrink-0 w-8 h-8 bg-primary-50 rounded-full flex items-center justify-center">
                    <AISparklesIcon className="w-5 h-5" />
                  </div>
                  <div className="flex flex-col gap-2 flex-1">
                    <div className="max-w-64 min-w-44 px-3 py-2.5 bg-neutral-50 rounded-tr-lg rounded-bl-lg rounded-br-lg flex flex-col gap-1">
                      <Loading dotClassName="bg-neutral-300" />
                      {showDelayMessage && (
                        <p className="animate-fade-in text-text-secondary text-xs mt-1">
                          AI 선생님이 고민 중이에요
                        </p>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* 스트리밍 중인 AI 메시지 */}
            <StreamingMessage questionId={question.questionId} />

            <div ref={messagesEndRef} />
          </div>
        )}
      </div>

      <div className="p-4 border-t border-neutral-100 shrink-0">
        <Input
          ref={inputRef}
          type="text"
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={placeholder}
          disabled={!isActive || isDisabled || isWaitingResponse || isStreaming}
          size="lg"
          className="disabled:border-neutral-200"
          rightIcon={
            <Button
              onClick={handleSend}
              disabled={
                !isActive || isDisabled || isWaitingResponse || isStreaming || !inputValue.trim()
              }
              variant="ghost"
              className="p-0 h-auto min-h-0 text-primary-600 hover:bg-transparent hover:opacity-80"
            >
              <Send className="w-5 h-5" />
            </Button>
          }
        />

        <p className="text-neutral-200 text-xs text-center mt-2.5">
          AI 답변은 오류가 있을 수 있으니 교차 검증을 권장합니다.
        </p>
      </div>
    </div>
  )
}
