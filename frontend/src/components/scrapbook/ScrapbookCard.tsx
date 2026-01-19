'use client'
import StartSolveCard from '@/components/shared/StartSolveCard'
import ScrapbookCardView from './ScrapbookCardView'
import useGetScrapbookCard from '@/hooks/scrapbook/use-get-scrapbook-card'

export default function ScrapbookCard() {
  const { data, isLoading } = useGetScrapbookCard()

  if (isLoading) {
    return (
      <StartSolveCard
        title={<>스크랩북을 불러오는 중이에요</>}
        description={<>잠시만 기다려 주세요.</>}
        label="불러오는 중…"
        disabled
      />
    )
  } else if (!data?.data || data.data.length === 0) {
    return (
      <StartSolveCard
        title={
          <>
            아직 스크랩한
            <span className="lg:block"> 오프너 분석이 없어요</span>
          </>
        }
        description={
          <>
            문제를 풀고 오프너 분석을 하면
            <span className="lg:block"> AI의 질문을 따라 도달한 풀이가 자동으로 모여요.</span>
          </>
        }
        label="첫 문제 풀어보기"
      />
    )
  }

  return (
    <section className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-4 gap-4">
      {data.data.map((scrapbookData) => (
        <ScrapbookCardView key={scrapbookData.examResultId} {...scrapbookData} />
      ))}
    </section>
  )
}
