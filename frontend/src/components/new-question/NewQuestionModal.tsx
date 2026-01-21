'use client'
import { useState } from 'react'
import { Modal, ModalContent, ModalFooter, ModalHeader } from '@/components/common/Modal'
import ResultBanner from '@/components/shared/ResultBanner'
import NewQuestionLoading from './NewQuestionLoading'
import NewQuestionHeader from './NewQuestionHeader'
import NewQuestionAnswer from './NewQuestionAnswer'
import NewQuestionAnalysis from './NewQuestionAnalysis'
import NewQuestionAction from './NewQuestionAction'
import NewQuestionExam from './NewQuestionExam'
import { useGenerateQuestion } from '@/hooks/exam/queries/use-generate-question'
import { useSubmitResult } from '@/hooks/exam/queries/use-submit-answer'
import { useExamStore } from '@/stores/use-exam-store'
import useCurrentExam from '@/hooks/exam/use-current-exam'

interface NewQuestionModalProps {
  open: boolean
  onClose: () => void
}

export default function NewQuestionModal({ open, onClose }: NewQuestionModalProps) {
  const examData = useCurrentExam()
  const { currentIndex } = useExamStore()
  const questionId = examData?.questions[currentIndex]?.questionId ?? 0
  const submitResult = useSubmitResult(questionId)
  const questionResultId = submitResult?.questionResultId ?? 0
  const { data, isFetching } = useGenerateQuestion({ questionId, questionResultId })

  // 문제 제출하기
  const [isSubmitted, setSubmitted] = useState<boolean>(false)
  // 문제 정답 상태
  const [isCorrect, setIsCorrect] = useState<boolean | null>(null)
  // 선택지 선택 상태
  const [selected, setIsSelected] = useState<number | null>(null)
  const [frqAnswer, setFrqAnswer] = useState<number | null>(null)

  // 변형문제는 MCQ만 지원 (options가 있으면 MCQ)
  const questionType = data?.options ? 'MCQ' : 'FRQ'
  // passages 배열의 첫 번째 content를 passage로 사용
  const passage = data?.passages?.[0]?.content ?? ''

  const handleSubmit = () => {
    if (!data) return
    const answer = questionType === 'MCQ' ? selected : frqAnswer
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
      {isFetching ? (
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
                <NewQuestionExam passage={passage} />
              </ModalContent>

              {/* 구분선 */}
              <div className="border-t border-t-neutral-200 my-4 lg:my-6" />

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
                  type={questionType}
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
                type={questionType}
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
