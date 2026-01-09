import { RESULT_MENU, resultData } from '@/mocks/exam-complete-mocks'
import Image from 'next/image'
export default function ExamResultSummary() {
  return (
    <>
      <div className="flex flex-col items-center gap-5 p-6">
        <Image src="/icons/partypopper.svg" alt={'학습 종료 축하 아이콘'} width={60} height={60} />
        <h1 className="text-2xl text-text-primary font-bold">모든 문제 풀이를 마쳤어요</h1>
        <span className="text-text-secondary">학습 결과를 한눈에 확인해보세요</span>
      </div>
      {RESULT_MENU.map(({ label, key }) => (
        <div
          key={key}
          className="flex justify-between items-center border-b p-4  border-neutral-200 font-bold h-12"
        >
          <div>{label}</div>
          <div>{resultData[key]}</div>
        </div>
      ))}
    </>
  )
}
