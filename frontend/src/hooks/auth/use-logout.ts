import { API_PATHS } from '@/constants/api-path'
import { QUERY_KEYS } from '@/constants/query-key'
import { useExamStore } from '@/stores/use-exam-store'
import api from '@/utils/api'
import { useMutation } from '@tanstack/react-query'

async function logoutApi() {
  return api(API_PATHS.AUTH.LOGOUT, { method: 'POST' }, false)
}

export default function useLogout() {
  return useMutation({
    mutationKey: QUERY_KEYS.AUTH.LOGOUT,
    mutationFn: logoutApi,
    onSettled: () => {
      useExamStore.getState().resetExam()
    },
  })
}
