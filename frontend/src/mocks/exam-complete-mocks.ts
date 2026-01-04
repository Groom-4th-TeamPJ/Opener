import { ResultData } from '@/types/exam-complete'
export const resultData: ResultData = {
  solveTime: '1:36:12',
  correctCount: 27,
  wrongCount: 3,
  openerCount: 5,
}

export const RESULT_MENU: { label: string; key: keyof ResultData }[] = [
  { label: '풀이 시간', key: 'solveTime' },
  { label: '정답 수', key: 'correctCount' },
  { label: '오답 수', key: 'wrongCount' },
  { label: '오프너 분석 수', key: 'openerCount' },
]
