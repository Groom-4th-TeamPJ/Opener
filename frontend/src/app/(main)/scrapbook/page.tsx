import AnalysisCard from '@/components/scrapbook/AnalysisCard'
import StartSolveCard from '@/components/shared/StartSolveCard'
import { SCRAPBOOK_MOCK_DATA } from '@/mocks/scrapbook-data'

// TODO: 스크랩북 조회 API 연결
export default function Scrapbook() {
  return (
    <div className="fixed inset-0 top-16 overflow-y-auto">
      <div className="min-h-full flex justify-center items-center py-8">
        <main className="w-full max-w-6xl px-4">
          <section className="mb-10">
            <h1 className="text-[1.75rem] font-bold text-text-primary">내 스크랩북</h1>
            <h2 className="text-sm sm:text-base text-text-secondary">
              지금까지 저장한 오프너 분석 내용을 확인할 수 있어요
            </h2>
          </section>
          {SCRAPBOOK_MOCK_DATA.length > 0 ? (
            <section className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-4 gap-4">
              {SCRAPBOOK_MOCK_DATA.map(
                ({ userResultId, year, examType, analysisCount, recentDate }) => (
                  <AnalysisCard
                    key={userResultId}
                    userResultId={userResultId}
                    year={year}
                    examType={examType}
                    analysisCount={analysisCount}
                    recentDate={recentDate}
                  />
                )
              )}
            </section>
          ) : (
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
          )}
        </main>
      </div>
    </div>
  )
}
