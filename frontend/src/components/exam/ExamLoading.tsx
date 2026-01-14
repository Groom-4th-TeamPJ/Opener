import Image from 'next/image'

export default function ExamLoading() {
  return (
    <div className="fixed inset-0 top-16 bg-neutral-50 flex items-center justify-center">
      <div className="flex flex-col items-center text-text-primary font-medium">
        <Image
          src="/icons/solid_exam_primary.svg"
          alt="로딩 중"
          width={90}
          height={90}
          className="animate-bounce-scale mb-4"
        />
        <span>선택한 문제를 불러오고 있어요.</span>
        <span>잠시만 기다려주세요.</span>
      </div>
    </div>
  )
}
