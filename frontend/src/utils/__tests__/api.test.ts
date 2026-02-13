import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest'
import { server, http, HttpResponse } from '@/mocks/server'

vi.mock('sonner', () => ({
  toast: { error: vi.fn() },
}))
import { toast } from 'sonner'
import { apiFail, apiOk } from '@/mocks/utils/api-response'
import { API_PATHS } from '@/constants/api-path'

const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? 'https://opener.ai.kr/api'

const expectedSummaryData = {
  userName: '홍길동',
  monthlyAverageCorrectRate: 78,
  monthlyQuestionsSolvedCount: 32,
  totalLearningTimeDesc: '1일 1시간 1분',
  totalQuestionsSolvedCount: 32,
}

const expectedCanData = {
  currentCan: 10,
}

// 헬퍼
async function freshApi() {
  // 모듈 리셋 후 재임포트
  vi.resetModules()
  const mod = await import('@/utils/api')
  return mod.default
}

function mockLocation() {
  Object.defineProperty(window, 'location', {
    value: { href: '' },
    writable: true,
  })
}

// 테스트 코드
describe('api wrapper 테스트', () => {
  beforeEach(() => {
    vi.useRealTimers()
    vi.clearAllMocks()
    mockLocation()
  })

  afterEach(() => {
    // 남아있는 타이머 정리 (setTimeout 800ms 등)
    try {
      vi.runOnlyPendingTimers()
    } catch {
      // 에러 방지용 주석
    }
    vi.useRealTimers()
    server.resetHandlers()
  })

  test('401이면 refresh 성공 후 원 요청을 재시도해서 성공한다', async () => {
    const api = await freshApi()

    let protectedCallCount = 0
    let refreshCallCount = 0

    server.use(
      http.get(`${BASE_URL}${API_PATHS.DASHBOARD.SUMMARY}`, () => {
        protectedCallCount += 1
        if (protectedCallCount === 1) {
          return apiFail('unauthorized', 401)
        }
        return apiOk(expectedSummaryData)
      }),
      http.post(`${BASE_URL}${API_PATHS.AUTH.REFRESH}`, () => {
        refreshCallCount += 1
        return HttpResponse.json(null, { status: 200 })
      })
    )

    const res = await api(API_PATHS.DASHBOARD.SUMMARY, { method: 'GET' })
    expect(res).toEqual(expectedSummaryData)
    expect(protectedCallCount).toBe(2) // 401 + 재시도
    expect(refreshCallCount).toBe(1) // refresh 1회
    expect(toast.error).not.toHaveBeenCalled()
    expect(window.location.href).toBe('')
  })

  test('refresh가 실패하면: throw 발생, 800ms 뒤 toast + /login 이동', async () => {
    const api = await freshApi()
    vi.useFakeTimers()

    server.use(
      http.get(`${BASE_URL}${API_PATHS.DASHBOARD.SUMMARY}`, () => apiFail('unauthorized', 401)),
      http.post(`${BASE_URL}${API_PATHS.AUTH.REFRESH}`, () =>
        HttpResponse.json({}, { status: 401 })
      )
    )

    const promise = api(API_PATHS.DASHBOARD.SUMMARY, { method: 'GET' })

    // throw 확인
    await expect(promise).rejects.toBeDefined()

    expect(toast.error).not.toHaveBeenCalled()
    expect(window.location.href).toBe('')

    // 800ms 뒤 toast + redirect
    await vi.advanceTimersByTimeAsync(800)

    expect(toast.error).toHaveBeenCalledTimes(1)
    expect(toast.error).toHaveBeenCalledWith('세션이 만료되었습니다. 다시 로그인해주세요.', {
      duration: 3000,
    })
    expect(window.location.href).toBe('/login')
  })

  test('동시에 401이 여러 개 떠도 refresh는 1번만 실행된다', async () => {
    const api = await freshApi()
    let refreshCallCount = 0
    let p1Count = 0
    let p2Count = 0

    server.use(
      http.get(`${BASE_URL}${API_PATHS.DASHBOARD.SUMMARY}`, () => {
        p1Count += 1
        if (p1Count === 1) return apiFail('unauthorized', 401)
        return apiOk(expectedSummaryData)
      }),
      http.get(`${BASE_URL}${API_PATHS.USERS.ME}`, () => {
        p2Count += 1
        if (p2Count === 1) return apiFail('unauthorized', 401)
        return apiOk(expectedCanData)
      }),
      http.post(`${BASE_URL}${API_PATHS.AUTH.REFRESH}`, async () => {
        refreshCallCount += 1
        return HttpResponse.json(null, { status: 200 })
      })
    )

    const [r1, r2] = await Promise.all([
      api(API_PATHS.DASHBOARD.SUMMARY, { method: 'GET' }),
      api(API_PATHS.USERS.ME, { method: 'GET' }),
    ])
    expect(r1).toEqual(expectedSummaryData)
    expect(r2).toEqual(expectedCanData)
    expect(refreshCallCount).toBe(1)
    expect(p1Count).toBe(2)
    expect(p2Count).toBe(2)
  })

  test('동시에 refresh 실패가 여러 번 발생해도 toast/redirect는 1번만 실행된다', async () => {
    const api = await freshApi()
    vi.useFakeTimers()

    server.use(
      http.get(`${BASE_URL}${API_PATHS.DASHBOARD.SUMMARY}`, () => apiFail('unauthorized', 401)),
      http.get(`${BASE_URL}${API_PATHS.USERS.ME}`, () => apiFail('unauthorized', 401)),
      http.post(`${BASE_URL}${API_PATHS.AUTH.REFRESH}`, () =>
        HttpResponse.json({}, { status: 401 })
      )
    )

    const [r1, r2] = await Promise.allSettled([
      api(API_PATHS.DASHBOARD.SUMMARY, { method: 'GET' }, true),
      api(API_PATHS.USERS.ME, { method: 'GET' }, true),
    ])

    expect(r1.status).toBe('rejected')
    expect(r2.status).toBe('rejected')

    // 800ms 전에는 UI 효과 없음
    expect(toast.error).toHaveBeenCalledTimes(0)
    expect(window.location.href).toBe('')

    await vi.advanceTimersByTimeAsync(800)

    // sessionExpiredHandled 가드 검증
    expect(toast.error).toHaveBeenCalledTimes(1)
    expect(window.location.href).toBe('/login')
  })
})
