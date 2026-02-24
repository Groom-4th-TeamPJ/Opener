import createPresetHandler from '@/mocks/preset-factory'
import { apiFail, apiOk } from '../utils/api-response'
import { API_PATHS } from '@/constants/api-path'

const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? 'https://opener.ai.kr/api'

export const dashboardSummaryHandlers = createPresetHandler(
  'get',
  `${BASE_URL}${API_PATHS.DASHBOARD.SUMMARY}`,
  [
    {
      label: '200 - 대시보드 요약 성공',
      resolver: () =>
        apiOk(
          {
            userName: '홍길동',
            monthlyAverageCorrectRate: 78,
            monthlyQuestionsSolvedCount: 32,
            totalLearningTimeDesc: '1일 1시간 1분',
            totalQuestionsSolvedCount: 32,
          },
          'summary ok',
          200
        ),
    },
    {
      label: '401 - 대시보드 요약 실패 (인증)',
      resolver: () => apiFail('unauthorized', 401),
    },
  ]
)

export const userCanHandlers = createPresetHandler('get', `${BASE_URL}${API_PATHS.USERS.ME}`, [
  {
    label: '200 - 캔 개수 조회 성공',
    resolver: () =>
      apiOk(
        {
          currentCan: 10,
        },
        'cans count ok',
        200
      ),
  },
  {
    label: '401 - 캔 개수 조회 실패 (인증)',
    resolver: () => apiFail('unauthorized', 401),
  },
])
