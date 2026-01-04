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

export type Option = {
  order: number
  text: string
}
type ExamVariantValues = {
  data: {
    questionNewId: number
    category: string
    type: string
    passage: string
    options: Option[]
    answer: number
    analysis: unknown
  }
}
//목데이터
const MOCK_DATA: ExamVariantValues = {
  data: {
    questionNewId: 2,
    category: 'GEO',
    type: 'MCQ',
    passage:
      '수열 \( b_n \)을  \[b_n = a_n + a_{n+1} \quad (n \ge 1)\]이라 하자. 두 집합\[A = \{a_1, a_2, a_3, a_4, a_5\}, \quadB = \{b_1, b_2, b_3, b_4, b_5\}\] 에 대하여\[n(A \cap B) = 2\]가 되도록 하는 모든 수열 \( a_n \)에 대하여  \[a_{15}\]의 값의 합을 구하시오.',

    options: [
      {
        order: 1,
        text: '-1',
      },
      {
        order: 2,
        text: '0',
      },
      {
        order: 3,
        text: '1',
      },
      {
        order: 4,
        text: '-2',
      },
      {
        order: 5,
        text: '10',
      },
    ],
    answer: 3,
    analysis: {},
  },
}
export default function ExamVariant() {
  // 데이터 불러오는 상태
  const [loading, setLoading] = useState(false)
  //   문제 불러오기
  const [data, setData] = useState<ExamVariantValues>(MOCK_DATA)
  //   문제 제출하기
  const [isSubmitted, setSubmitted] = useState<boolean>(false)
  //   문제 정답 상태
  const [isCorrect, setIsCorrect] = useState<boolean | null>(null)
  //   선택지 선택 상태
  const [selected, setIsSelected] = useState<number | null>(null)
  const router = useRouter()
  //모달 닫기 상태
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
    <div>
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
    </div>
  )
}
