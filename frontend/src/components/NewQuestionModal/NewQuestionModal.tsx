'use client'
import { useState } from 'react'
import { Modal, ModalContent, ModalFooter, ModalHeader } from '@/components/common/Modal'
import { useRouter } from 'next/navigation'
import ResultBanner from '@/components/shared/ResultBanner'
import { MOCK_DATA } from '@/mocks/exam-variant-mocks'
import type { NewQuestionModalValues } from '@/types/exam-variant'
import NewQuestionLoading from '@/components/NewQuestionModal/NewQuestionLoading'
import NewQuestionHeader from '@/components/NewQuestionModal/NewQuestionHeader'
import NewQuestionAnswer from '@/components/NewQuestionModal/NewQuestionAnswer'
import NewQuestionAnalysis from '@/components/NewQuestionModal/NewQuestionAnalysis'
import NewQuestionExam from '@/components/NewQuestionModal/NewQuestionExam'
import NewQuestionAction from '@/components/NewQuestionModal/NewQuestionAction'

export default function NewQuestionModal() {
  // 데이터 불러오는 상태 (현재 Mock 데이터를 사용하므로 set 함수 제외)
  const [loading] = useState(false)
  //   문제 불러오기 (현재 Mock 데이터를 사용하므로 set 함수 제외)
  const [data] = useState<NewQuestionModalValues | null>(MOCK_DATA)
  //   문제 제출하기
  const [isSubmitted, setSubmitted] = useState<boolean>(false)
  //   문제 정답 상태
  const [isCorrect, setIsCorrect] = useState<boolean | null>(null)
  //   선택지 선택 상태
  const [selected, setIsSelected] = useState<number | null>(null)

  const router = useRouter()

  const handleClose = () => {
    router.back()
  }

  const handleSubmit = () => {
    if (selected === null) {
      return alert('문제를 선택해주세요!')
    }
    setSubmitted(true)
    setIsCorrect(selected === data?.data.answer)
  }
  // 원래 문제로 돌아가기
  const handleBack = () => {
    router.back()
  }

  return (
    <Modal open={true} onClose={handleClose} className="md:max-w-145 lg:max-w-198">
      {loading ? (
        <NewQuestionLoading />
      ) : (
        data && (
          <div>
            <ModalHeader closable={true} onClose={handleClose} className="flex">
              <NewQuestionHeader />
            </ModalHeader>
            {/* 문제 */}
            <ModalContent className="flex flex-col gap-4 ">
              <NewQuestionExam passage={data.data.passage} />
              {/* 정오답 표시 배너 */}
              {isSubmitted && isCorrect !== null && (
                <ResultBanner result={isCorrect ? 'correct' : 'wrong'} />
              )}
            </ModalContent>

            {/* 구분선 */}
            <div className="border-t border-t-neutral-200" />

            <ModalContent className="flex flex-col gap-4">
              <h3 className="font-bold text-neutral-600">답안 선택</h3>
              {/* 답안 선택지 */}
              <NewQuestionAnswer
                options={data.data.options}
                answer={data.data.answer}
                selected={selected}
                isSubmitted={isSubmitted}
                onSelect={setIsSelected}
              />
              {/* 오답 해설 */}
              <NewQuestionAnalysis
                analysis={data.data.analysis}
                isSubmitted={isSubmitted}
                isCorrect={isCorrect}
              />
            </ModalContent>

            {/* 제출버튼 및 원래 페이지로 돌아가기 */}
            <ModalFooter>
              <NewQuestionAction
                isSubmitted={isSubmitted}
                selected={selected}
                onSubmit={handleSubmit}
                onBack={handleBack}
              />
            </ModalFooter>
          </div>
        )
      )}
    </Modal>
  )
}
