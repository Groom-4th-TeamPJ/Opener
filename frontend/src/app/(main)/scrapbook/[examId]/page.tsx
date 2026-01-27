import ScrapBookList from '@/components/scrapbook/ScrapBookList'
import { Metadata } from 'next'

export const metadata: Metadata = {
  title: '스크랩북 문제 목록',
  description: '선택한 시험의 스크랩 문제 목록입니다',
}

export default function UserResult() {
  return (
    <>
      <ScrapBookList />
    </>
  )
}
