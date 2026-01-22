'use client'
import { useState, useEffect, useRef } from 'react'
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
import { toast } from 'sonner'
import type { StopwatchRef } from '@/types/exam'

interface NewQuestionModalProps {
  open: boolean
  onClose: () => void
}

export default function NewQuestionModal({ open, onClose }: NewQuestionModalProps) {
  const stopwatchRef = useRef<StopwatchRef>(null)
  const examData = useCurrentExam()
  const { currentIndex, updateQuestionState } = useExamStore()
  const questionId = examData?.questions[currentIndex]?.questionId ?? 0
  const submitResult = useSubmitResult(questionId)
  const questionResultId = submitResult?.questionResultId ?? 0
  const { data, isFetching, isError } = useGenerateQuestion({ questionId, questionResultId })

  // 요청 실패 시 모달 닫기 및 버튼 다시 활성화
  useEffect(() => {
    if (isError) {
      toast.error('변형문제 생성에 실패했습니다. 다시 시도해주세요.', { duration: 3000 })
      updateQuestionState(questionId, { hasNewQuestion: false })
      onClose()
    }
  }, [isError, onClose, questionId, updateQuestionState])

  // 문제 제출하기
  const [isSubmitted, setSubmitted] = useState<boolean>(false)
  // 문제 정답 상태
  const [isCorrect, setIsCorrect] = useState<boolean | null>(null)
  // 선택지 선택 상태
  const [selected, setIsSelected] = useState<number | null>(null)
  const [frqAnswer, setFrqAnswer] = useState<number | null>(null)

  // 모달이 열릴 때마다 상태를 초기화
  useEffect(() => {
    if (open) {
      setSubmitted(false)
      setIsCorrect(null)
      setIsSelected(null)
      setFrqAnswer(null)
    }
    // 모달이 닫힐 때 스톱워치 리셋
    stopwatchRef.current?.reset()
  }, [open])

  // 변형문제는 MCQ만 지원 (options가 있으면 MCQ)
  const questionType = data?.options ? 'MCQ' : 'FRQ'
  // passages 배열의 첫 번째 content를 passage로 사용
  const passage = data?.passages?.[0]?.content ?? ''

  const handleSubmit = () => {
    if (!data) return
    const answer = questionType === 'MCQ' ? selected : frqAnswer
    if (answer == null) return

    stopwatchRef.current?.stop()
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
      lg:max-w-198 lg:h-198 overflow-hidden"
    >
      {isFetching ? (
        <NewQuestionLoading />
      ) : (
        data && (
          <div className="flex flex-col h-full">
            <ModalHeader
              closable={true}
              onClose={onClose}
              className="flex border-b border-neutral-200"
            >
              <NewQuestionHeader stopwatchRef={stopwatchRef} />
            </ModalHeader>
            {/* 문제 */}
            <div className="flex-1 min-h-0 overflow-y-auto scrollbar-overlay">
              <ModalContent className="flex flex-col gap-4 p-4 2xl:p-6 min-h-67">
                <NewQuestionExam passage={passage} />
              </ModalContent>

              {/* 구분선 */}
              <div className="border-t border-t-neutral-200" />

              <ModalContent className="flex flex-col gap-4 p-4 2xl:p-6">
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
            <ModalFooter className="border-t border-neutral-200">
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
