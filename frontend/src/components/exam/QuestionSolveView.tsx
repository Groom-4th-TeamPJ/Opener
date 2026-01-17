'use client'

import { useEffect, useRef } from 'react'
import { useRouter } from 'next/navigation'
import type { StopwatchRef } from '@/types/exam'
import { ROUTES } from '@/constants/routes'
import ExamHeader from './ExamHeader'
import AnswerCard from './AnswerCard'
import QuestionActionButton from './QuestionActionButton'
import NavigationButton from './NavigationButton'
import useInactivityDetection from '@/hooks/exam/use-inactivity-detection'
import QuestionCard from './QuestionCard'
import AIChatbot from '@/components/shared/AIChatbot'
import InactivityModal from '@/components/exam/InactivityModal'
import NewQuestionModal from '@/components/new-question/NewQuestionModal'
import ExamExitModal from './ExamExitModal'
import ExamResultModal from './ExamResultModal'
import useCurrentExam from '@/hooks/exam/use-current-exam'
import usePreventRefresh from '@/hooks/exam/use-prevent-refresh'
import { useSSEChat } from '@/hooks/exam/use-sse-chat'
import { useSubmitAnswer } from '@/hooks/exam/queries/use-submit-answer'
import { useExamModalStore } from '@/stores/use-exam-modal-store'
import { useExamStore } from '@/stores/use-exam-store'
import { EXAM_MODAL } from '@/constants/exam'

interface QuestionSolveViewProps {
  onClose: () => void
}

// 비활성 타임아웃
const INACTIVITY_TIMEOUT = 60 * 60 * 1000

export default function QuestionSolveView({ onClose }: QuestionSolveViewProps) {
  const router = useRouter()
  const stopwatchRef = useRef<StopwatchRef>(null)
  const { openModal, closeModal, isOpen } = useExamModalStore()
  const { currentIndex, goNextQuestion, updateQuestionState, getQuestionState } = useExamStore()
  const { mutate: submitAnswer, isPending: isSubmitting } = useSubmitAnswer()
  const examData = useCurrentExam()

  // 마운트 시 모달 상태 초기화
  useEffect(() => {
    closeModal()
  }, [closeModal])

  // SSE 연결 (examResultId가 있을 때만 연결)
  useSSEChat({ sessionId: examData?.examResultId ?? 0, enabled: !!examData?.examResultId })

  // 비활성 감지
  useInactivityDetection({
    timeout: INACTIVITY_TIMEOUT,
    enabled: !!examData && !isOpen(EXAM_MODAL.RESULT),
    onInactive: () => {
      openModal(EXAM_MODAL.INACTIVITY)
    },
  })

  // 새로고침 감지
  usePreventRefresh({
    enabled: !!examData && !isOpen(EXAM_MODAL.RESULT),
    onPrevent: () => {
      openModal(EXAM_MODAL.EXIT)
    },
  })

  // 문제 변경 시 타이머 리셋
  useEffect(() => {
    if (!examData) return
    stopwatchRef.current?.reset()
  }, [currentIndex, examData])

  // 캐시에 데이터가 없으면 선택 화면으로 복귀
  useEffect(() => {
    if (!examData) {
      onClose()
    }
  }, [examData, onClose])

  // React 훅 규칙: 모든 훅 호출 끝난 후 early return
  if (!examData) return null

  const { questions } = examData
  const currentQuestion = questions[currentIndex]
  const isLastQuestion = currentIndex === questions.length - 1
  const questionState = getQuestionState(currentQuestion.questionId)

  const handleSubmit = () => {
    if (isSubmitting) return
    if (currentQuestion.questionType === 'MCQ' && questionState.selectedChoice === null) return
    if (currentQuestion.questionType === 'FRQ' && questionState.frqAnswer.trim() === '') return

    // 답안 제출 시 스탑워치 정지
    stopwatchRef.current?.stop()
    const timeSpent = stopwatchRef.current?.getTime() ?? 0

    const selected =
      currentQuestion.questionType === 'MCQ'
        ? questionState.selectedChoice!
        : Number(questionState.frqAnswer)

    submitAnswer(
      {
        examResultId: examData.examResultId,
        questionId: currentQuestion.questionId,
        body: { selected, timeSpent },
      },
      {
        onSuccess: (data) => {
          updateQuestionState(currentQuestion.questionId, {
            isSubmitted: true,
            isCorrect: data?.correct ?? false,
            correctAnswer: data?.answer ?? null,
          })
        },
      }
    )
  }

  const handleNext = () => {
    if (isLastQuestion) {
      // 시험 완료 - 결과 모달 표시
      openModal(EXAM_MODAL.RESULT)
    } else {
      goNextQuestion()
    }
  }

  const handleShowAnalysis = () => {
    updateQuestionState(currentQuestion.questionId, { isAnalysisActive: true })
    // TODO: AI 분석 요청
  }

  // TODO: 비활성 상태 60분 자동으로 대시보드 이동하도록
  const handleInactivityConfirm = () => {
    router.push(ROUTES.DASHBOARD)
  }

  // 변형 문제 풀기 모달 열기
  const handleVariationClick = () => {
    openModal(EXAM_MODAL.NEW_QUESTION)
    updateQuestionState(currentQuestion.questionId, { hasNewQuestion: true })
  }

  // 결과 모달 닫기
  const handleResultModalClose = () => {
    closeModal()
    onClose()
  }

  // 콘텐츠 보호: 우클릭, 드래그, 복사 차단
  const handleContextMenu = (e: React.MouseEvent) => {
    e.preventDefault()
  }

  const handleCopy = (e: React.ClipboardEvent) => {
    e.preventDefault()
  }

  const handleDragStart = (e: React.DragEvent) => {
    e.preventDefault()
  }

  return (
    <div
      className="h-screen flex flex-col bg-background select-none"
      onContextMenu={handleContextMenu}
      onCopy={handleCopy}
      onDragStart={handleDragStart}
    >
      {/* Top Header Bar */}
      <ExamHeader onClose={() => openModal(EXAM_MODAL.EXIT)} />

      {/* Main Content */}
      <div className="flex-1 overflow-y-auto bg-neutral-50">
        <div className="w-full min-h-full max-w-6xl mx-auto px-4 md:px-8 py-6 flex items-stretch gap-6">
          {/* Left: Question + Answer */}
          <div className="flex-1 flex flex-col gap-6 min-w-86 min-h-0 overflow-hidden">
            <QuestionCard stopwatchRef={stopwatchRef} />

            <AnswerCard />

            {/* 버튼 + 네비게이션 */}
            <div className="flex gap-4">
              <QuestionActionButton
                onSubmit={handleSubmit}
                onShowAnalysis={handleShowAnalysis}
                onVariationClick={handleVariationClick}
              />
              <div className="shrink-0">
                <NavigationButton isLastQuestion={isLastQuestion} onNext={handleNext} />
              </div>
            </div>
          </div>

          {/* Right: AI Chatbot */}
          <AIChatbot
            isActive={questionState.isAnalysisActive}
            question={currentQuestion}
            selectedChoice={questionState.selectedChoice}
            frqAnswer={questionState.frqAnswer}
          />
        </div>
      </div>

      {/* 변형 문제 모달 */}

      <NewQuestionModal open={isOpen(EXAM_MODAL.NEW_QUESTION)} onClose={closeModal} />

      {/* 비활성 모달 */}
      <InactivityModal open={isOpen(EXAM_MODAL.INACTIVITY)} onConfirm={handleInactivityConfirm} />

      {/* 이탈 경고 모달 */}
      <ExamExitModal open={isOpen(EXAM_MODAL.EXIT)} onCancel={closeModal} onConfirm={onClose} />

      {/* 학습 결과 모달 */}
      <ExamResultModal open={isOpen(EXAM_MODAL.RESULT)} onClose={handleResultModalClose} />
    </div>
  )
}
