'use client'

import { useState, useEffect, useRef } from 'react'
import { useRouter } from 'next/navigation'
import type { ExamResponse, StopwatchRef } from '@/types/exam'
import { ROUTES } from '@/constants/routes'
import ExamHeader from './ExamHeader'
import AnswerCard from './AnswerCard'
import QuestionActionButton from './QuestionActionButton'
import NavigationButton from './NavigationButton'
import useInactivityDetection from '@/hooks/exam/use-inactivity-detection'
import QuestionCard from './QuestionCard'
import AIChatbot from './AIChatbot'
import InactivityModal from '@/components/shared/InactivityModal'
import NewQuestionModal from '@/components/new-question/NewQuestionModal'
import ExamExitModal from './ExamExitModal'
import Button from '@/components/common/Button'
import cn from '@/utils/cn'

interface QuestionSolveProps {
  data: ExamResponse
  onClose: () => void
}

// 비활성 타임아웃
const INACTIVITY_TIMEOUT = 60 * 60 * 1000

export default function QuestionSolveView({ data, onClose }: QuestionSolveProps) {
  const router = useRouter()
  const { exam, questions } = data

  const [currentIndex, setCurrentIndex] = useState(0)
  const [selectedChoice, setSelectedChoice] = useState<number | null>(null)
  const [frqAnswer, setFrqAnswer] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [isCorrect, setIsCorrect] = useState<boolean | null>(null)
  const [isAnalysisActive, setIsAnalysisActive] = useState(false)
  const [showInactivityModal, setShowInactivityModal] = useState(false)
  const [showVariationModal, setShowVariationModal] = useState(false)
  const [showExitModal, setShowExitModal] = useState(false)
  const [hasNewQuestion, setHasNewQuestion] = useState(false)

  // 스탑워치 ref
  const stopwatchRef = useRef<StopwatchRef>(null)

  // 비활성 감지
  useInactivityDetection({
    timeout: INACTIVITY_TIMEOUT,
    onInactive: () => {
      setShowInactivityModal(true)
      setShowVariationModal(false)
      setShowExitModal(false)
    },
  })

  const currentQuestion = questions[currentIndex]
  const isLastQuestion = currentIndex === questions.length - 1

  // 문제 변경 시 타이머 리셋
  useEffect(() => {
    stopwatchRef.current?.reset()
  }, [currentIndex])

  const handleChoiceSelect = (index: number) => {
    if (submitted || currentQuestion.type === 'FRQ') return
    setSelectedChoice(index)
  }

  const handleSubmit = () => {
    if (currentQuestion.type === 'MCQ' && selectedChoice === null) return
    if (currentQuestion.type === 'FRQ' && frqAnswer.trim() === '') return

    // 답안 제출 시 스탑워치 정지
    stopwatchRef.current?.stop()

    const correct =
      currentQuestion.type === 'MCQ'
        ? selectedChoice === currentQuestion.answer
        : Number(frqAnswer) === currentQuestion.answer
    setIsCorrect(correct)
    setSubmitted(true)
  }

  const handleNext = () => {
    if (isLastQuestion) {
      // 시험 완료 - 대시보드로 이동
      router.push(ROUTES.DASHBOARD)
    } else {
      setCurrentIndex((prev) => prev + 1)
      setSelectedChoice(null)
      setFrqAnswer('')
      setSubmitted(false)
      setIsCorrect(null)
      setIsAnalysisActive(false)
      setHasNewQuestion(false)
    }
  }

  const handleShowAnalysis = () => {
    setIsAnalysisActive(true)
    // TODO: AI 분석 요청
  }

  // 비활성 모달 확인 시 처리
  const handleInactivityConfirm = () => {
    router.push(ROUTES.DASHBOARD)
  }

  // 변형 문제 풀기 모달 열기
  const handleVariationClick = () => {
    setShowVariationModal(true)
    setHasNewQuestion(true)
  }

  // 변형 문제 모달 닫기
  const handleVariationModalClose = () => {
    setShowVariationModal(false)
  }

  // 이탈 경고 모달 - 계속 학습하기
  const handleExitCancel = () => {
    setShowExitModal(false)
  }

  // 이탈 경고 모달 - 학습 종료하기
  const handleExitConfirm = () => {
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
      <ExamHeader onClose={() => setShowExitModal(true)} canCount={10} />

      {/* Main Content */}
      <div className="flex-1 overflow-y-auto bg-neutral-50">
        <div className="w-full max-w-6xl mx-auto px-4 md:px-8 py-6 flex items-stretch gap-6 min-h-full">
          {/* Left: Question + Answer */}
          <div className="flex-1 flex flex-col gap-6 min-w-86">
            <QuestionCard
              exam={exam}
              question={currentQuestion}
              stopwatchRef={stopwatchRef}
              submitted={submitted}
              isCorrect={isCorrect}
            />

            <AnswerCard
              question={currentQuestion}
              selectedChoice={selectedChoice}
              frqAnswer={frqAnswer}
              submitted={submitted}
              isCorrect={isCorrect}
              onChoiceSelect={handleChoiceSelect}
              onFrqAnswerChange={setFrqAnswer}
            />

            {/* 데스크톱: 제출 버튼만 */}
            <div className="hidden xl:block">
              <QuestionActionButton
                submitted={submitted}
                selectedChoice={selectedChoice}
                frqAnswer={frqAnswer}
                questionType={currentQuestion.type}
                isAnalysisActive={isAnalysisActive}
                isCorrect={isCorrect}
                hasNewQuestion={hasNewQuestion}
                onSubmit={handleSubmit}
                onShowAnalysis={handleShowAnalysis}
                onVariationClick={handleVariationClick}
              />
            </div>

            {/* Next Button - Floating (데스크톱) */}
            <div className="hidden xl:block fixed top-1/2 -translate-y-1/2 xl:left-[calc(50%+36rem)] 2xl:left-[calc(50%+36rem-2rem+5rem)]">
              <NavigationButton
                isLastQuestion={isLastQuestion}
                submitted={submitted}
                onNext={handleNext}
              />
            </div>

            {/* 태블릿: 버튼 + 네비게이션 */}
            <div className="flex xl:hidden gap-4">
              <QuestionActionButton
                submitted={submitted}
                selectedChoice={selectedChoice}
                frqAnswer={frqAnswer}
                questionType={currentQuestion.type}
                isAnalysisActive={isAnalysisActive}
                isCorrect={isCorrect}
                hasNewQuestion={hasNewQuestion}
                onSubmit={handleSubmit}
                onShowAnalysis={handleShowAnalysis}
                onVariationClick={handleVariationClick}
              />
              <div className="shrink-0">
                <Button
                  onClick={submitted ? handleNext : undefined}
                  disabled={!submitted}
                  variant="outline"
                  size="lg"
                  className={cn(
                    'w-20 border-neutral-200 hover:border-neutral-200',
                    !submitted && 'cursor-not-allowed opacity-50'
                  )}
                >
                  {isLastQuestion ? '학습종료' : '다음'}
                </Button>
              </div>
            </div>
          </div>

          {/* Right: AI Chatbot */}
          <AIChatbot
            isActive={isAnalysisActive}
            question={currentQuestion}
            selectedChoice={selectedChoice}
            frqAnswer={frqAnswer}
          />
        </div>
      </div>

      {/* 변형 문제 모달 */}
      {showVariationModal && (
        <NewQuestionModal open={showVariationModal} onClose={handleVariationModalClose} />
      )}

      {/* 비활성 모달 */}
      <InactivityModal open={showInactivityModal} onConfirm={handleInactivityConfirm} />

      {/* 이탈 경고 모달 */}
      <ExamExitModal
        open={showExitModal}
        onCancel={handleExitCancel}
        onConfirm={handleExitConfirm}
      />
    </div>
  )
}
