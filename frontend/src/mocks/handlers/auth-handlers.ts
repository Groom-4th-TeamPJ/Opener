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
      resolver: () => apiFail('이미 사용 중인 이메일입니다', 401),
    },
  ]
)

export const refreshHandlers = createPresetHandler('post', `${BASE_URL}${API_PATHS.AUTH.REFRESH}`, [
  { label: '200 - refresh 성공', resolver: () => apiOk(null, 'refresh ok', 200) },
  { label: '401 - refresh 실패', resolver: () => apiFail('expired', 401) },
])
