import ExamSelect from '@/components/exam/ExamSelect'
import { Metadata } from 'next'

export const metadata: Metadata = {
  title: '문제 풀이',
  description: '시험 카테고리와 연도를 선택해 문제를 풀어보세요',
}

export default function ExamPage() {
  return <ExamSelect />
}
