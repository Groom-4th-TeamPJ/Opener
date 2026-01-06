import PartyPopperIcon from '@/icons/partyPopperIcon'
import { RESULT_MENU, resultData } from '@/mocks/exam-complete-mocks'

export default function ExamResultSummary() {
  return (
    <>
      <div className="flex flex-col items-center gap-5 p-6">
        <PartyPopperIcon className="lg:w-20 h-20" />
        <h1 className="text-2xl font-bold">모든 문제 풀이를 마쳤어요</h1>
        <span>학습 결과를 한눈에 확인해보세요</span>
      </div>

      {RESULT_MENU.map(({ label, key }) => (
        <div
          key={key}
          className="flex justify-between items-center border p-4 rounded-lg border-neutral-200 font-bold h-12"
        >
          <div>{label}</div>
          <div>{resultData[key]}</div>
        </div>
      ))}
    </>
  )
}
