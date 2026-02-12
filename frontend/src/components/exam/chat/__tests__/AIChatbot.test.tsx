/* eslint-disable no-console */
import { describe, test, expect, beforeEach, afterEach, vi } from 'vitest'
import { render, waitFor, cleanup } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import AIChatbot, { measurementData, resetMeasurement } from '@/components/exam/chat/AIChatbot'
import { useExamStore } from '@/stores/use-exam-store'
import { setSSEMockConfig, resetSSEMockConfig } from '@/mocks/handlers/sse'
import { useSSEChat } from '@/hooks/exam/use-sse-chat'
import type { Question, ChatMessage } from '@/types/exam'

/**
 * 스트리밍 조건 설정
 * - 시나리오 1, 2 모두 동일한 조건으로 측정해야 비교 가능
 */
const STREAMING_CONFIG = {
  chunkCount: 57,
  delayMs: 26, // 1490ms / 57 ≈ 26ms
}

function AIChatbotWithSSE({
  sessionId,
  question,
  isActive,
}: {
  sessionId: number
  question: Question
  isActive: boolean
}) {
  useSSEChat({
    sessionId,
    questionId: question.questionId,
    enabled: true,
  })

  return <AIChatbot isActive={isActive} question={question} />
}

function createMockQuestion(overrides?: Partial<Question>): Question {
  return {
    questionId: 1,
    questionNo: 1,
    point: 2,
    questionType: 'MCQ',
    passages: [{ order: 1, type: 'TEXT', content: '테스트 문제입니다.' }],
    options: [
      { order: 1, content: '선택지 1' },
      { order: 2, content: '선택지 2' },
    ],
    answer: 1,
    ...overrides,
  }
}

function createMockChatMessage(
  id: number,
  role: 'USER' | 'ASSISTANT',
  content: string
): ChatMessage {
  return {
    id,
    role,
    content,
    timestamp: new Date().toISOString(),
  }
}

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  })

  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  }
}

async function waitForStreamingComplete(
  questionId: number,
  expectedMessageCount: number,
  timeout = 10000
) {
  await waitFor(
    () => {
      const state = useExamStore.getState()
      const questionState = state.questionStates[questionId]
      expect(state.streamingMessages[questionId] ?? '').toBe('')
      expect(questionState?.chatMessages.length).toBe(expectedMessageCount)
    },
    { timeout }
  )
}

describe('AIChatbot 렌더링 성능 측정', () => {
  const originalConsoleError = console.error

  beforeEach(() => {
    // SSE 관련 에러 로그 숨기기
    console.error = vi.fn((...args: unknown[]) => {
      const message = String(args[0])
      if (message.includes('[SSE]')) return
      originalConsoleError(...args)
    })

    useExamStore.getState().resetExam()
    useExamStore.getState().setExam({ examYear: 2024, category: 'test', examType: 'test' }, 1)
    resetSSEMockConfig()
    resetMeasurement()
  })

  afterEach(() => {
    console.error = originalConsoleError
    cleanup()
  })

  test('시나리오1: 첫 AI 응답 (기존 메시지 없음)', async () => {
    const chunks = Array(STREAMING_CONFIG.chunkCount).fill('T')
    setSSEMockConfig({ chunks, delayMs: STREAMING_CONFIG.delayMs })

    const question = createMockQuestion()
    const stateUpdates: number[] = []
    let streamingStartTime = 0
    let streamingEndTime = 0

    const unsubscribe = useExamStore.subscribe((state) => {
      const streaming = state.streamingMessages[question.questionId] ?? ''
      if (streaming && stateUpdates.length === 0) {
        streamingStartTime = performance.now()
      }
      if (streaming) {
        stateUpdates.push(performance.now())
      }
    })

    render(<AIChatbotWithSSE sessionId={1} question={question} isActive={true} />, {
      wrapper: createWrapper(),
    })

    await waitForStreamingComplete(question.questionId, 1)
    streamingEndTime = performance.now()
    unsubscribe()

    const streamingDuration = streamingEndTime - streamingStartTime
    const stateUpdateRate = stateUpdates.length / (streamingDuration / 1000)

    console.log('\n=========== 📊 시나리오1: 첫 AI 응답 ===========')
    console.log(`스트리밍 시간: ${(streamingDuration / 1000).toFixed(2)}초`)
    console.log('')
    console.log('◆ AIChatbot')
    console.log(`  렌더 횟수: ${measurementData.renderCount}`)
    console.log('')
    console.log('◆ State Update (SSE 청크)')
    console.log(`  총 청크 수: ${stateUpdates.length}`)
    console.log(`  초당 청크: ${stateUpdateRate.toFixed(2)} chunks/sec`)
    console.log('================================================\n')

    expect(measurementData.renderCount).toBeGreaterThan(0)
    expect(stateUpdates.length).toBe(STREAMING_CONFIG.chunkCount)
  })

  test('시나리오2: 추가 질문 (기존 메시지 있음)', async () => {
    const chunks = Array(STREAMING_CONFIG.chunkCount).fill('T')
    const question = createMockQuestion()

    // 기존 메시지 설정: 첫 AI 응답 + 사용자 질문
    const existingMessages: ChatMessage[] = [
      createMockChatMessage(1, 'ASSISTANT', 'T'.repeat(57)),
      createMockChatMessage(2, 'USER', '9'),
    ]

    // store에 기존 메시지 설정
    useExamStore.getState().getQuestionState(question.questionId)
    existingMessages.forEach((msg) => {
      useExamStore.getState().addChatMessage(question.questionId, msg)
    })

    setSSEMockConfig({ chunks, delayMs: STREAMING_CONFIG.delayMs })

    const stateUpdates: number[] = []
    let streamingStartTime = 0
    let streamingEndTime = 0

    const unsubscribe = useExamStore.subscribe((state) => {
      const streaming = state.streamingMessages[question.questionId] ?? ''
      if (streaming && stateUpdates.length === 0) {
        streamingStartTime = performance.now()
      }
      if (streaming) {
        stateUpdates.push(performance.now())
      }
    })

    render(<AIChatbotWithSSE sessionId={1} question={question} isActive={true} />, {
      wrapper: createWrapper(),
    })

    // 기존 메시지 2개 + 새 스트리밍 메시지 1개 = 3개
    await waitForStreamingComplete(question.questionId, existingMessages.length + 1)
    streamingEndTime = performance.now()
    unsubscribe()

    const streamingDuration = streamingEndTime - streamingStartTime
    const stateUpdateRate = stateUpdates.length / (streamingDuration / 1000)

    console.log('\n=========== 📊 시나리오2: 추가 질문 ===========')
    console.log(`기존 메시지: ${existingMessages.length}개`)
    console.log(`스트리밍 시간: ${(streamingDuration / 1000).toFixed(2)}초`)
    console.log('')
    console.log('◆ AIChatbot')
    console.log(`  렌더 횟수: ${measurementData.renderCount}`)
    console.log('')
    console.log('◆ State Update (SSE 청크)')
    console.log(`  총 청크 수: ${stateUpdates.length}`)
    console.log(`  초당 청크: ${stateUpdateRate.toFixed(2)} chunks/sec`)
    console.log('================================================\n')

    expect(measurementData.renderCount).toBeGreaterThan(0)
    expect(stateUpdates.length).toBe(STREAMING_CONFIG.chunkCount)
  })

  test('시나리오 비교: 기존 메시지 유무에 따른 렌더링 비용 차이', async () => {
    const chunks = Array(STREAMING_CONFIG.chunkCount).fill('T')

    // ========== 시나리오 1: 기존 메시지 없음 ==========
    const question1 = createMockQuestion({ questionId: 101 })
    setSSEMockConfig({ chunks, delayMs: STREAMING_CONFIG.delayMs })

    const stateUpdates1: number[] = []
    let start1 = 0

    const unsub1 = useExamStore.subscribe((state) => {
      const streaming = state.streamingMessages[question1.questionId] ?? ''
      if (streaming && stateUpdates1.length === 0) start1 = performance.now()
      if (streaming) stateUpdates1.push(performance.now())
    })

    const { unmount: unmount1 } = render(
      <AIChatbotWithSSE sessionId={1} question={question1} isActive={true} />,
      { wrapper: createWrapper() }
    )

    await waitForStreamingComplete(question1.questionId, 1)
    const end1 = performance.now()
    unsub1()
    const renderCount1 = measurementData.renderCount
    const duration1 = end1 - start1
    unmount1()

    // ========== 시나리오 2: 기존 메시지 있음 ==========
    useExamStore.getState().resetExam()
    useExamStore.getState().setExam({ examYear: 2024, category: 'test', examType: 'test' }, 1)
    resetMeasurement()

    const question2 = createMockQuestion({ questionId: 102 })
    const existingMessages: ChatMessage[] = [
      createMockChatMessage(1, 'ASSISTANT', 'T'.repeat(57)),
      createMockChatMessage(2, 'USER', '9'),
    ]

    useExamStore.getState().getQuestionState(question2.questionId)
    existingMessages.forEach((msg) => {
      useExamStore.getState().addChatMessage(question2.questionId, msg)
    })

    setSSEMockConfig({ chunks, delayMs: STREAMING_CONFIG.delayMs })

    const stateUpdates2: number[] = []
    let start2 = 0

    const unsub2 = useExamStore.subscribe((state) => {
      const streaming = state.streamingMessages[question2.questionId] ?? ''
      if (streaming && stateUpdates2.length === 0) start2 = performance.now()
      if (streaming) stateUpdates2.push(performance.now())
    })

    const { unmount: unmount2 } = render(
      <AIChatbotWithSSE sessionId={1} question={question2} isActive={true} />,
      { wrapper: createWrapper() }
    )

    await waitForStreamingComplete(question2.questionId, existingMessages.length + 1)
    const end2 = performance.now()
    unsub2()
    const renderCount2 = measurementData.renderCount
    const duration2 = end2 - start2
    unmount2()

    // ========== 비교 결과 출력 ==========
    const renderDiff = renderCount2 - renderCount1

    console.log('\n=========== 📊 시나리오 비교 결과 ===========')
    console.table([
      {
        시나리오: '1 (메시지 없음)',
        '렌더 횟수': renderCount1,
        'State Update': stateUpdates1.length,
        '스트리밍 시간(s)': (duration1 / 1000).toFixed(2),
      },
      {
        시나리오: '2 (메시지 2개)',
        '렌더 횟수': renderCount2,
        'State Update': stateUpdates2.length,
        '스트리밍 시간(s)': (duration2 / 1000).toFixed(2),
      },
    ])
    console.log('')
    console.log(`📈 렌더 횟수 차이: ${renderDiff > 0 ? '+' : ''}${renderDiff}`)
    console.log('')

    if (renderDiff <= 5) {
      console.log('✅ 기존 메시지가 스트리밍 중 리렌더되지 않음 (최적화됨)')
    } else {
      console.log('⚠️ 기존 메시지가 스트리밍 중 리렌더됨 (최적화 필요)')
    }
    console.log('==============================================\n')

    expect(renderCount1).toBeGreaterThan(0)
    expect(renderCount2).toBeGreaterThan(0)
  })
})
