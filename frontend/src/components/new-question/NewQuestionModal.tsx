'use client'
import { useState } from 'react'
import { Modal, ModalContent, ModalFooter, ModalHeader } from '@/components/common/Modal'
import ResultBanner from '@/components/shared/ResultBanner'
import { MOCK_DATA } from '@/mocks/exam-variant-mocks'
import type { NewQuestion } from '@/types/exam-variant'
import NewQuestionLoading from './NewQuestionLoading'
import NewQuestionHeader from './NewQuestionHeader'
import NewQuestionAnswer from './NewQuestionAnswer'
import NewQuestionAnalysis from './NewQuestionAnalysis'
import NewQuestionAction from './NewQuestionAction'
import NewQuestionExam from './NewQuestionExam'

interface NewQuestionModalProps {
  open: boolean
  onClose: () => void
}

export default function NewQuestionModal({ open, onClose }: NewQuestionModalProps) {
  // 데이터 불러오는 상태 (현재 Mock 데이터를 사용하므로 set 함수 제외)
  const [loading] = useState(false)
  //   문제 불러오기 (현재 Mock 데이터를 사용하므로 set 함수 제외)
  const [data] = useState<NewQuestion | null>(MOCK_DATA.data[0])
  //   문제 제출하기
  const [isSubmitted, setSubmitted] = useState<boolean>(false)
  //   문제 정답 상태
  const [isCorrect, setIsCorrect] = useState<boolean | null>(null)
  //   선택지 선택 상태
  const [selected, setIsSelected] = useState<number | null>(null)
  const [frqAnswer, setFrqAnswer] = useState<number | null>(null)

  const handleSubmit = () => {
    if (!data) return
    const answer = data.type === 'MCQ' ? selected : frqAnswer
    if (answer == null) return

    setSubmitted(true)
    setIsCorrect(answer === data.answer)
  }

  const handleChange = (answer: number) => {
    setFrqAnswer(answer)
  }
  return (
    <Modal
      open={open}
      onClose={onClose}
      zIndex={150}
      className="h-145 max-w-145 
       lg:max-w-198 lg:h-198"
    >
      {loading ? (
        <NewQuestionLoading />
      ) : (
        data && (
          <div className="flex flex-col h-full">
            <ModalHeader closable={true} onClose={onClose} className="flex">
              <NewQuestionHeader />
            </ModalHeader>
            {/* 문제 */}
            <div className="flex-1 min-h-0 overflow-y-auto">
              <ModalContent className="flex flex-col gap-4 ">
                <NewQuestionExam passage={data.passage} />
              </ModalContent>

              {/* 구분선 */}
              <div className="border-t border-t-neutral-200 mt-4 mb-4 lg:mt-6 lg:mb-6" />

              <ModalContent className="flex flex-col gap-4">
                <div className="flex items-center justify-between">
                  <h3 className="font-bold text-neutral-600">답안 선택</h3>
                  {/* 정오답 표시 */}
                  {isSubmitted && isCorrect !== null && (
                    <ResultBanner result={isCorrect ? 'correct' : 'wrong'} />
                  )}
                </div>
                {/* 답안 선택지 */}
                <NewQuestionAnswer
                  type={data.type}
                  options={data.options ?? []}
                  answer={data.answer}
                  selected={selected}
                  frqAnswer={frqAnswer}
                  isSubmitted={isSubmitted}
                  onSelect={setIsSelected}
                  onFrqChange={handleChange}
                />
                {/* 오답 해설 */}
                <NewQuestionAnalysis
                  analysis={data.analysis}
                  isSubmitted={isSubmitted}
                  isCorrect={isCorrect}
                />
              </ModalContent>
            </div>
            {/* 제출버튼 및 원래 페이지로 돌아가기 */}
            <ModalFooter>
              <NewQuestionAction
                type={data.type}
                isSubmitted={isSubmitted}
                selected={selected}
                frqAnswer={frqAnswer}
                onSubmit={handleSubmit}
                onBack={onClose}
              />
            </ModalFooter>
          </div>
        )
      )}
    </Modal>
  )
}
