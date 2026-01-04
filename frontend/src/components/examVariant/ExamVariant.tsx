'use client'
import { useState } from 'react'
import { Modal, ModalContent, ModalFooter } from '../common/Modal'
import { useRouter } from 'next/navigation'
import LoadingSection from './LoadingSection'
import QuestionSection from './QuestionSection'
import ResultBanner from './ResultBanner'
import AnalysisSection from './AnalysisSection'
import AnswerSection from './AnswerSection'
import ActionSection from './ActionSection'
import { MOCK_DATA } from '@/mocks/exam-variant-mocks'
import type { ExamVariantValues } from '@/types/exam-variant'

export default function ExamVariant() {
  // 데이터 불러오는 상태 (현재 Mock 데이터를 사용하므로 set 함수 제외)
  const [loading] = useState(false)
  //   문제 불러오기 (현재 Mock 데이터를 사용하므로 set 함수 제외)
  const [data] = useState<ExamVariantValues | null>(MOCK_DATA)
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
    <Modal open={true} onClose={handleClose}>
      {loading ? (
        <LoadingSection />
      ) : (
        data && (
          <div>
            {/* 문제 */}
            <QuestionSection passage={data.data.passage} onClose={handleClose} />

            {/* 정오답 표시 배너 */}
            <ResultBanner isSubmitted={isSubmitted} isCorrect={isCorrect} />
            {/* 구분선 */}
            <div className="border-t border-t-neutral-200 mt-4" />

            <ModalContent>
              <div className="flex flex-col gap-2">
                <h3 className="font-bold text-neutral-600">답안 선택</h3>
                {/* 답안 선택지 */}
                <AnswerSection
                  options={data.data.options}
                  answer={data.data.answer}
                  selected={selected}
                  isSubmitted={isSubmitted}
                  onSelect={setIsSelected}
                />
              </div>
            </ModalContent>
            {/* 오답 해설 */}
            <AnalysisSection
              analysis={data.data.analysis}
              isSubmitted={isSubmitted}
              isCorrect={isCorrect}
            />

            {/* 제출버튼 및 원래 페이지로 돌아가기 */}
            <ModalFooter>
              <ActionSection
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
