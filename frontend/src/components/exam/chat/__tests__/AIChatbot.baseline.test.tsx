/* eslint-disable no-console */
import { Profiler, type ProfilerOnRenderCallback } from 'react'
import { describe, test, expect, beforeEach, afterEach, vi } from 'vitest'
import { render, waitFor, cleanup } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import AIChatbot from '@/components/exam/chat/AIChatbot'
import { useExamStore } from '@/stores/use-exam-store'
import { setSSEMockConfig, resetSSEMockConfig } from '@/mocks/handlers/sse'
import { useSSEChat } from '@/hooks/exam/use-sse-chat'
import type { Question } from '@/types/exam'

/**
 * 실제 실험 조건과 동일한 설정
 * - 총 청크 수: 57개
 * - 스트리밍 시간: ~1.49초
 * - 초당 청크: ~38.26 chunks/sec
 */
const EXPERIMENT_CONFIG = {
  chunkCount: 57,
  delayMs: 26, // 1490ms / 57 ≈ 26ms
  expectedDurationMs: 1490,
}

interface ProfileData {
  id: string
  phase: 'mount' | 'update'
  actualDuration: number
  baseDuration: number
  startTime: number
  commitTime: number
}

interface MeasurementResult {
  renderCount: number
  mountCount: number
  updateCount: number
  totalActualDuration: number
  totalBaseDuration: number
  renders: ProfileData[]
}

function createProfiler() {
  const data: ProfileData[] = []

  const onRender: ProfilerOnRenderCallback = (
    id,
    phase,
    actualDuration,
    baseDuration,
    startTime,
    commitTime
  ) => {
    data.push({
      id,
      phase: phase as 'mount' | 'update',
      actualDuration,
      baseDuration,
      startTime,
      commitTime,
    })
  }

  const getResult = (): MeasurementResult => ({
    renderCount: data.length,
    mountCount: data.filter((d) => d.phase === 'mount').length,
    updateCount: data.filter((d) => d.phase === 'update').length,
    totalActualDuration: data.reduce((sum, d) => sum + d.actualDuration, 0),
    totalBaseDuration: data.reduce((sum, d) => sum + d.baseDuration, 0),
    renders: [...data],
  })

  const reset = () => {
    data.length = 0
  }

  return { onRender, getResult, reset }
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

async function waitForStreamingComplete(questionId: number, timeout = 10000) {
  await waitFor(
    () => {
      const state = useExamStore.getState()
      const questionState = state.questionStates[questionId]
      expect(questionState?.streamingMessage).toBe('')
      expect(questionState?.chatMessages.length).toBeGreaterThan(0)
    },
    { timeout }
  )
}

describe('Phase 1: 최적화 전 Baseline 측정', () => {
  const originalConsoleError = console.error

  beforeEach(() => {
    console.error = vi.fn((...args: unknown[]) => {
      const message = String(args[0])
      if (message.includes('[SSE]')) return
      originalConsoleError(...args)
    })

    useExamStore.getState().resetExam()
    useExamStore.getState().setExam({ examYear: 2024, category: 'test', examType: 'test' }, 1)
    resetSSEMockConfig()
  })

  afterEach(() => {
    console.error = originalConsoleError
    cleanup()
  })

  test('시나리오1: 오프너분석 클릭 시 첫 AI 응답 - Baseline 측정', async () => {
    const chunks = Array(EXPERIMENT_CONFIG.chunkCount).fill('테')
    setSSEMockConfig({ chunks, delayMs: EXPERIMENT_CONFIG.delayMs })

    const question = createMockQuestion()
    const profiler = createProfiler()
    const stateUpdates: number[] = []
    let streamingStartTime = 0
    let streamingEndTime = 0

    const unsubscribe = useExamStore.subscribe((state) => {
      const streaming = state.questionStates[question.questionId]?.streamingMessage ?? ''
      if (streaming && stateUpdates.length === 0) {
        streamingStartTime = performance.now()
      }
      if (streaming) {
        stateUpdates.push(performance.now())
      }
    })

    render(
      <Profiler id="AIChatbot" onRender={profiler.onRender}>
        <AIChatbotWithSSE sessionId={1} question={question} isActive={true} />
      </Profiler>,
      { wrapper: createWrapper() }
    )

    await waitForStreamingComplete(question.questionId)
    streamingEndTime = performance.now()
    unsubscribe()

    const result = profiler.getResult()
    const streamingDuration = streamingEndTime - streamingStartTime
    const stateUpdateRate = stateUpdates.length / (streamingDuration / 1000)

    console.log('\n=========== 📊 측정 결과 ===========')
    console.log(`스트리밍 시간: ${(streamingDuration / 1000).toFixed(2)}초 (첫 청크 ~ 마지막 청크)`)
    console.log('')
    console.log('◆ AIChatbot')
    console.log(`  렌더 횟수: ${result.renderCount}`)
    console.log(
      `  평균 렌더 시간: ${(result.totalActualDuration / result.renderCount).toFixed(2)}ms`
    )
    console.log('')
    console.log('◆ State Update (SSE 청크)')
    console.log(`  총 청크 수: ${stateUpdates.length}`)
    console.log(`  초당 청크: ${stateUpdateRate.toFixed(2)} chunks/sec`)
    console.log('=====================================\n')

    expect(result.renderCount).toBeGreaterThan(0)
    expect(stateUpdates.length).toBe(EXPERIMENT_CONFIG.chunkCount)
  })

  test('동일 조건 3회 반복 측정 - 재현성 검증', async () => {
    const chunks = Array(EXPERIMENT_CONFIG.chunkCount).fill('테')
    const runs: {
      renderCount: number
      avgRenderTime: number
      stateUpdateCount: number
      stateUpdateRate: number
      streamingDuration: number
    }[] = []

    for (let i = 0; i < 3; i++) {
      useExamStore.getState().resetExam()
      useExamStore.getState().setExam({ examYear: 2024, category: 'test', examType: 'test' }, 1)
      setSSEMockConfig({ chunks, delayMs: EXPERIMENT_CONFIG.delayMs })

      const question = createMockQuestion({ questionId: 200 + i })
      const profiler = createProfiler()
      const stateUpdates: number[] = []
      let streamingStartTime = 0

      const unsubscribe = useExamStore.subscribe((state) => {
        const streaming = state.questionStates[question.questionId]?.streamingMessage ?? ''
        if (streaming && stateUpdates.length === 0) {
          streamingStartTime = performance.now()
        }
        if (streaming) {
          stateUpdates.push(performance.now())
        }
      })

      const { unmount } = render(
        <Profiler id={`Run-${i}`} onRender={profiler.onRender}>
          <AIChatbotWithSSE sessionId={1} question={question} isActive={true} />
        </Profiler>,
        { wrapper: createWrapper() }
      )

      await waitForStreamingComplete(question.questionId)
      const streamingEndTime = performance.now()
      unsubscribe()

      const result = profiler.getResult()
      const streamingDuration = streamingEndTime - streamingStartTime

      runs.push({
        renderCount: result.renderCount,
        avgRenderTime: result.totalActualDuration / result.renderCount,
        stateUpdateCount: stateUpdates.length,
        stateUpdateRate: stateUpdates.length / (streamingDuration / 1000),
        streamingDuration,
      })

      unmount()
    }

    console.log('\n=========== 📊 3회 반복 측정 결과 ===========')
    console.table(
      runs.map((run, i) => ({
        '실행 #': i + 1,
        '렌더 횟수': run.renderCount,
        '평균 렌더 시간(ms)': run.avgRenderTime.toFixed(2),
        '청크 수': run.stateUpdateCount,
        '초당 청크': run.stateUpdateRate.toFixed(2),
        '스트리밍 시간(s)': (run.streamingDuration / 1000).toFixed(2),
      }))
    )

    const renderCounts = runs.map((r) => r.renderCount)
    const allEqual = renderCounts.every((c) => c === renderCounts[0])
    console.log(
      allEqual
        ? `✅ 재현성 확인: 모든 실행에서 동일한 렌더 횟수 (${renderCounts[0]})`
        : `⚠️ 렌더 횟수 불일치: ${renderCounts.join(', ')}`
    )
    console.log('==============================================\n')

    expect(runs.every((r) => r.stateUpdateCount === EXPERIMENT_CONFIG.chunkCount)).toBe(true)
  })
})
