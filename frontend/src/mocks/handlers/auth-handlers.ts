import { API_PATHS } from '@/constants/api-path'
import createPresetHandler from '@/mocks/preset-factory'
import { apiOk, apiFail } from '@/mocks/utils/api-response'

const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? 'https://opener.ai.kr/api'

export const loginHandlers = createPresetHandler(
  'post',
  `${BASE_URL}${API_PATHS.AUTH.FORM_LOGIN}`,
  [
    {
      label: '200 - 로그인 성공',
      resolver: () => apiOk(null, '로그인이 정상 처리되었습니다', 200),
    },
    { label: '401 - 로그인 실패', resolver: () => apiFail('아이디/비밀번호 오류', 401) },
    {
      label: '401 - 로그인 실패 (A_015)',
      resolver: () =>
        apiFail('계정이 잠겼습니다. 5분 후 다시 시도해주세요.', 401, {
          code: 'A_015',
          reason: '계정이 잠겼습니다. 5분 후 다시 시도해주세요.',
        }),
    },
  ]
)

export const signupHandlers = createPresetHandler(
  'post',
  `${BASE_URL}${API_PATHS.AUTH.FORM_REGISTER}`,
  [
    {
      label: '200 - 회원가입 성공',
      resolver: () => apiOk(null, '회원가입이 완료되었습니다', 200),
    },
    {
      label: '401 - 회원가입 실패 (중복/검증)',
      resolver: () =>
        apiFail('이미 존재하는 계정입니다.', 401, {
          code: 'S_001',
          reason: '이미 존재하는 계정입니다.',
        }),
    },
  ]
)

export const refreshHandlers = createPresetHandler('post', `${BASE_URL}${API_PATHS.AUTH.REFRESH}`, [
  { label: '200 - refresh 성공', resolver: () => apiOk(null, 'refresh ok', 200) },
  { label: '401 - refresh 실패', resolver: () => apiFail('expired', 401) },
])
