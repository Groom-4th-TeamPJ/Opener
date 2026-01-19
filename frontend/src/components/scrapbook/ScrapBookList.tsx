'use client'
import { Card, CardContent, CardHeader } from '@/components/common/Card'
import { useParams, useRouter, useSearchParams } from 'next/navigation'
import Image from 'next/image'
import Button from '@/components/common/Button'
import ScrapBookPagination from './ScrapBookPagination'
import ScrapBookListItem from './ScrapBookListItem'
import useScrapBookList from '@/hooks/scrapbook/use-scrapbook-list'

export default function ScrapBookList() {
  const router = useRouter()
  const examId = Number(useParams<{ examId: string }>().examId)
  const page = Number(useSearchParams().get('page') ?? 0)
  const { data: ScrapBookListData, isLoading, isError } = useScrapBookList({ examId, page })
  const questionResults = ScrapBookListData?.questionResults
  const totalPages = Math.max(1, questionResults?.totalPages ?? 0)
  const currentPage = page + 1 //UI에서 1,2,3...
  const previousPage = Math.max(1, currentPage - 1)
  const nextPage = Math.min(totalPages, currentPage + 1)
  if (isError) {
    throw new Error()
  }

  return (
    <div className="flex flex-col w-full gap-4 min-h-auto md:mt-6 lg:mt-10">
      <div className="flex gap-4 items-center">
        <Button
          variant="ghost"
          className="bg-none hover:bg-transparent"
          onClick={() => router.push('/scrapbook')}
        >
          <Image src="/icons/chevron-left.svg" alt="뒤로 가기" width={24} height={24} />
        </Button>

        <h1 className="text-text-primary text-[23px] font-bold">
          {ScrapBookListData?.examYear}년 {ScrapBookListData?.examType.name}
        </h1>
      </div>
      <Card>
        <CardHeader className="pb-0">
          <div className="grid grid-cols-[1fr_4fr_1fr] gap-5 border-b border-b-neutral-600 p-3 pt-0">
            <div>유형</div>
            <div>문제</div>
            <div>분석일시</div>
          </div>
        </CardHeader>

        <CardContent className="pt-0">
          {isLoading ? (
            <div className="flex justify-center items-center h-30 text-text-secondary">
              데이터를 불러오는 중...
            </div>
          ) : (
            questionResults?.content.map((history) => (
              <ScrapBookListItem
                key={history.questionResultId}
                questionResultId={history.questionResultId}
                categoryName={history.category.name}
                passage={history.passage}
                openerUsedAt={history.openerUsedAt}
              />
            ))
          )}
        </CardContent>
      </Card>
      <ScrapBookPagination
        currentPage={currentPage}
        previousPage={previousPage}
        nextPage={nextPage}
        totalPages={totalPages}
      />
    </div>
  )
}
