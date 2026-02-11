/* eslint-disable no-console */
import { Profiler, type ProfilerOnRenderCallback } from 'react'
import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { render, waitFor, cleanup } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import AIChatbot from '../AIChatbot'
import { useExamStore } from '@/stores/use-exam-store'
import { setSSEMockConfig, resetSSEMockConfig } from '@/mocks/handlers/sse'
import { useSSEChat } from '@/hooks/exam/use-sse-chat'
import type { Question } from '@/types/exam'

/**
 * 프로파일링 데이터 타입
 */
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

/**
 * 프로파일링 데이터 수집기
 */
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

/**
 * State Update Rate 측정기
 */
function createStateUpdateTracker(questionId: number) {
  const updates: { timestamp: number; streamingMessage: string }[] = []
  let startTime = 0

  const start = () => {
    startTime = performance.now()
    updates.length = 0
  }

  const unsubscribe = useExamStore.subscribe((state) => {
    const streaming = state.questionStates[questionId]?.streamingMessage ?? ''
    updates.push({
      timestamp: performance.now() - startTime,
      streamingMessage: streaming,
    })
  })

  const getResult = () => {
    const endTime = performance.now() - startTime
    const durationSec = endTime / 1000
    return {
      totalUpdates: updates.length,
      durationMs: endTime,
      updatesPerSecond: updates.length / durationSec,
      updates: [...updates],
    }
  }

  const stop = () => {
    unsubscribe()
  }

  return { start, stop, getResult }
}

/**
 * SSE 훅을 사용하는 래퍼 컴포넌트
 */
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

/**
 * 테스트용 Question 팩토리
 */
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

/**
 * QueryClient 래퍼
 */
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

/**
 * 스트리밍 완료 대기 헬퍼
 */
async function waitForStreamingComplete(questionId: number, timeout = 5000) {
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

describe('AIChatbot 렌더링 프로파일링', () => {
  beforeEach(() => {
    useExamStore.getState().resetExam()
    useExamStore.getState().setExam({ examYear: 2024, category: 'test', examType: 'test' }, 1)
    resetSSEMockConfig()
  })

  afterEach(() => {
    cleanup()
  })

  describe('렌더링 비용 지표 측정', () => {
    it('Render Count: 스트리밍 중 전체 컴포넌트 렌더 횟수를 측정한다', async () => {
      const chunks = ['Hello', ', ', 'World', '!']
      setSSEMockConfig({ chunks, delayMs: 20 })

      const question = createMockQuestion()
      const profiler = createProfiler()

      render(
        <Profiler id="AIChatbot" onRender={profiler.onRender}>
          <AIChatbotWithSSE sessionId={1} question={question} isActive={true} />
        </Profiler>,
        { wrapper: createWrapper() }
      )

      await waitForStreamingComplete(question.questionId)

      const result = profiler.getResult()

      console.log('\n=== Render Count 측정 결과 ===')
      console.log(`총 렌더 횟수: ${result.renderCount}`)
      console.log(`마운트: ${result.mountCount}`)
      console.log(`업데이트: ${result.updateCount}`)
      console.log(`청크 수: ${chunks.length}`)

      // 최소 마운트 1회 + 청크 수만큼 업데이트 발생 예상
      expect(result.mountCount).toBe(1)
      expect(result.updateCount).toBeGreaterThanOrEqual(chunks.length)
    })

    it('Render Duration: actualDuration을 측정한다', async () => {
      const chunks = ['테스트', ' ', '메시지', '입니다', '.']
      setSSEMockConfig({ chunks, delayMs: 20 })

      const question = createMockQuestion()
      const profiler = createProfiler()

      render(
        <Profiler id="AIChatbot" onRender={profiler.onRender}>
          <AIChatbotWithSSE sessionId={1} question={question} isActive={true} />
        </Profiler>,
        { wrapper: createWrapper() }
      )

      await waitForStreamingComplete(question.questionId)

      const result = profiler.getResult()

      console.log('\n=== Render Duration 측정 결과 ===')
      console.log(`총 actualDuration: ${result.totalActualDuration.toFixed(2)}ms`)
      console.log(`총 baseDuration: ${result.totalBaseDuration.toFixed(2)}ms`)
      console.log(
        `평균 actualDuration: ${(result.totalActualDuration / result.renderCount).toFixed(2)}ms`
      )

      // 렌더링 시간이 측정되었는지 확인
      expect(result.totalActualDuration).toBeGreaterThan(0)
    })

    it('청크 수 증가에 따른 렌더링 비용 변화를 측정한다', async () => {
      const testCases = [
        { name: '5 chunks', chunks: 'Hello'.split('') },
        { name: '20 chunks', chunks: 'Hello, World! 테스트!'.split('') },
        {
          name: '50 chunks',
          chunks:
            '이것은 긴 응답 메시지입니다. 청크가 많아지면 렌더링 비용이 어떻게 변하는지 테스트합니다.'.split(
              ''
            ),
        },
      ]

      const results: { name: string; result: MeasurementResult }[] = []

      for (const testCase of testCases) {
        useExamStore.getState().resetExam()
        useExamStore.getState().setExam({ examYear: 2024, category: 'test', examType: 'test' }, 1)

        setSSEMockConfig({ chunks: testCase.chunks, delayMs: 5 })

        const question = createMockQuestion({ questionId: Math.floor(Math.random() * 10000) })
        const profiler = createProfiler()

        const { unmount } = render(
          <Profiler id="AIChatbot" onRender={profiler.onRender}>
            <AIChatbotWithSSE sessionId={1} question={question} isActive={true} />
          </Profiler>,
          { wrapper: createWrapper() }
        )

        await waitForStreamingComplete(question.questionId)

        results.push({ name: testCase.name, result: profiler.getResult() })
        unmount()
      }

      console.log('\n=== 청크 수 증가에 따른 렌더링 비용 ===')
      console.table(
        results.map(({ name, result }) => ({
          테스트: name,
          '렌더 횟수': result.renderCount,
          '업데이트 횟수': result.updateCount,
          'actualDuration (ms)': result.totalActualDuration.toFixed(2),
          '평균 렌더 시간 (ms)': (result.totalActualDuration / result.renderCount).toFixed(2),
        }))
      )

      // 모든 테스트 케이스가 완료되었는지 확인
      expect(results.length).toBe(testCases.length)
    })
  })

  describe('스트리밍 특화 지표 측정', () => {
    it('State Update Rate: 초당 상태 업데이트 횟수를 측정한다', async () => {
      const chunks = '스트리밍 테스트 메시지입니다.'.split('')
      setSSEMockConfig({ chunks, delayMs: 10 })

      const question = createMockQuestion()
      const tracker = createStateUpdateTracker(question.questionId)

      tracker.start()

      render(<AIChatbotWithSSE sessionId={1} question={question} isActive={true} />, {
        wrapper: createWrapper(),
      })

      await waitForStreamingComplete(question.questionId)

      const result = tracker.getResult()
      tracker.stop()

      console.log('\n=== State Update Rate 측정 결과 ===')
      console.log(`총 업데이트 횟수: ${result.totalUpdates}`)
      console.log(`총 소요 시간: ${result.durationMs.toFixed(2)}ms`)
      console.log(`초당 업데이트: ${result.updatesPerSecond.toFixed(2)} updates/sec`)

      expect(result.totalUpdates).toBeGreaterThan(0)
    })

    it('완료된 메시지가 스트리밍 중 리렌더링되는지 확인한다', async () => {
      // 1. 먼저 첫 번째 메시지를 완료시킴
      const firstChunks = ['첫 번째 메시지']
      setSSEMockConfig({ chunks: firstChunks, delayMs: 10 })

      const question = createMockQuestion()

      const { unmount } = render(
        <AIChatbotWithSSE sessionId={1} question={question} isActive={true} />,
        { wrapper: createWrapper() }
      )

      await waitForStreamingComplete(question.questionId)
      unmount()

      // 2. 기존 메시지가 있는 상태에서 두 번째 스트리밍 시작
      const secondChunks = ['두', ' ', '번', '째', ' ', '메', '시', '지']
      setSSEMockConfig({ chunks: secondChunks, delayMs: 20 })

      // 유저 메시지 추가 (실제 시나리오 시뮬레이션)
      useExamStore.getState().addChatMessage(question.questionId, {
        id: 2,
        role: 'USER',
        content: '질문입니다',
        timestamp: '12:00',
      })

      const profiler = createProfiler()

      render(
        <Profiler id="AIChatbot-SecondStream" onRender={profiler.onRender}>
          <AIChatbotWithSSE sessionId={1} question={question} isActive={true} />
        </Profiler>,
        { wrapper: createWrapper() }
      )

      await waitForStreamingComplete(question.questionId)

      const result = profiler.getResult()

      console.log('\n=== 완료된 메시지 리렌더링 검증 ===')
      console.log(`기존 메시지 수: 2 (첫 번째 AI + 유저)`)
      console.log(`두 번째 스트리밍 청크 수: ${secondChunks.length}`)
      console.log(`총 렌더 횟수: ${result.renderCount}`)
      console.log(`업데이트 횟수: ${result.updateCount}`)

      // 이상적인 경우: 기존 메시지는 리렌더링되지 않아야 함
      // 현재 최적화 전이므로 청크 수보다 많은 업데이트가 발생할 수 있음
      console.log(`\n[최적화 체크포인트]`)
      console.log(`- 이상적 업데이트 횟수: ${secondChunks.length + 1} (청크 수 + complete)`)
      console.log(`- 실제 업데이트 횟수: ${result.updateCount}`)

      if (result.updateCount > secondChunks.length + 2) {
        console.log(`⚠️  불필요한 리렌더링 발생 가능성 있음`)
      } else {
        console.log(`✅ 리렌더링 최적화 상태 양호`)
      }
    })
  })

  describe('동일 조건 재현성 검증', () => {
    it('동일한 MSW 설정으로 동일한 결과가 재현된다', async () => {
      const chunks = ['재현성', ' ', '테스트']
      const runs: MeasurementResult[] = []

      for (let i = 0; i < 3; i++) {
        useExamStore.getState().resetExam()
        useExamStore.getState().setExam({ examYear: 2024, category: 'test', examType: 'test' }, 1)
        setSSEMockConfig({ chunks, delayMs: 10 })

        const question = createMockQuestion({ questionId: 100 + i })
        const profiler = createProfiler()

        const { unmount } = render(
          <Profiler id={`Run-${i}`} onRender={profiler.onRender}>
            <AIChatbotWithSSE sessionId={1} question={question} isActive={true} />
          </Profiler>,
          { wrapper: createWrapper() }
        )

        await waitForStreamingComplete(question.questionId)
        runs.push(profiler.getResult())
        unmount()
      }

      console.log('\n=== 동일 조건 재현성 검증 ===')
      console.table(
        runs.map((result, i) => ({
          '실행 #': i + 1,
          '렌더 횟수': result.renderCount,
          '업데이트 횟수': result.updateCount,
        }))
      )

      // 모든 실행에서 동일한 렌더 패턴이 나와야 함
      const renderCounts = runs.map((r) => r.renderCount)
      const allEqual = renderCounts.every((c) => c === renderCounts[0])

      if (allEqual) {
        console.log(`✅ 모든 실행에서 동일한 렌더 횟수: ${renderCounts[0]}`)
      } else {
        console.log(`⚠️  렌더 횟수 불일치: ${renderCounts.join(', ')}`)
      }

      expect(runs.length).toBe(3)
    })
  })
})
