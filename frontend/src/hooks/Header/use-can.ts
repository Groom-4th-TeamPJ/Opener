import { API_PATHS } from '@/constants/api-path'
import { canCount } from '@/types/can'

import apiJson from '@/utils/api'

export async function getCanCount() {
  return apiJson<canCount>(API_PATHS.USERS.ME, { method: 'GET' })
}
