'use client'

import { useState, useEffect, useRef } from 'react'
import { useRouter } from 'next/navigation'
import Button from '@/components/common/Button'
import { X } from 'lucide-react'
import type { ExamResponse } from '@/types/exam'
import { ROUTES } from '@/constants/routes'
import QuestionHeader from './QuestionHeader'
import QuestionCard from './QuestionCard'
import QuestionActionButton from './QuestionActionButton'
import NavigationButton from './NavigationButton'

interface QuestionSolveProps {
  data: ExamResponse
  onClose: () => void
}

export default function QuestionSolveView({ data, onClose }: QuestionSolveProps) {
  const router = useRouter()
  const { exam, questions } = data

  const [currentIndex, setCurrentIndex] = useState(0)
  const [selectedChoice, setSelectedChoice] = useState<number | null>(null)
  const [frqAnswer, setFrqAnswer] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [isCorrect, setIsCorrect] = useState<boolean | null>(null)
  const [isAnalysisActive, setIsAnalysisActive] = useState(false)

  // 스탑워치 상태
  const [elapsedSeconds, setElapsedSeconds] = useState(0)
  const [isTimerRunning, setIsTimerRunning] = useState(true)
  const timerRef = useRef<NodeJS.Timeout | null>(null)

  const currentQuestion = questions[currentIndex]
  const isLastQuestion = currentIndex === questions.length - 1

  // 스탑워치 타이머
  useEffect(() => {
    if (!isTimerRunning) return

    const intervalId = setInterval(() => {
      setElapsedSeconds((prev) => prev + 1)
    }, 1000)

    timerRef.current = intervalId

    return () => {
      clearInterval(intervalId)
    }
  }, [currentIndex, isTimerRunning])

  // 문제 변경 시 타이머 리셋
  useEffect(() => {
    setElapsedSeconds(0)
    setIsTimerRunning(true)
  }, [currentIndex])

  const handleChoiceSelect = (index: number) => {
    if (submitted || currentQuestion.type === 'FRQ') return
    setSelectedChoice(index)
  }

  const handleSubmit = () => {
    if (currentQuestion.type === 'MCQ' && selectedChoice === null) return
    if (currentQuestion.type === 'FRQ' && frqAnswer.trim() === '') return

    // 답안 제출 시 스탑워치 정지
    setIsTimerRunning(false)
    if (timerRef.current) {
      clearInterval(timerRef.current)
    }

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
    }
  }

  const handleShowAnalysis = () => {
    setIsAnalysisActive(true)
    // TODO: AI 분석 요청
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
      <header className="shrink-0 bg-background/80 backdrop-blur-sm border-b border-foreground/10">
        <div className="container max-w-7xl mx-auto px-4 h-14 flex items-center justify-between">
          {/* Left: X Button */}
          <Button
            variant="ghost"
            onClick={onClose}
            className="p-2 rounded-full text-foreground/60 hover:bg-red-600/10 hover:text-red-600 transition-all"
          >
            <X className="w-5 h-5" />
          </Button>

          {/* Right: Can Currency - TODO */}
          <div className="flex items-center gap-2">
            <div className="w-6 h-6 bg-foreground/10 rounded" />
            <span className="text-sm font-medium">10</span>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <div className="flex-1 overflow-y-auto flex items-center">
        <div className="container max-w-7xl mx-auto px-4 py-6 w-full">
          <div className="flex items-start justify-between">
            {/* Center: Question & Chat Panel */}
            <div className="flex gap-6 items-start flex-1 justify-center">
              {/* Left: Question */}
              <div className="w-110 flex flex-col gap-5">
                <QuestionHeader
                  exam={exam}
                  question={currentQuestion}
                  elapsedSeconds={elapsedSeconds}
                />
                <QuestionCard
                  question={currentQuestion}
                  selectedChoice={selectedChoice}
                  frqAnswer={frqAnswer}
                  submitted={submitted}
                  isCorrect={isCorrect}
                  onChoiceSelect={handleChoiceSelect}
                  onFrqAnswerChange={setFrqAnswer}
                />

                <div className="sticky bottom-0 bg-background -mb-6">
                  <QuestionActionButton
                    submitted={submitted}
                    selectedChoice={selectedChoice}
                    frqAnswer={frqAnswer}
                    questionType={currentQuestion.type}
                    isAnalysisActive={isAnalysisActive}
                    onSubmit={handleSubmit}
                    onShowAnalysis={handleShowAnalysis}
                  />
                </div>
              </div>

              {/* Center: AI Chat Panel - TODO */}
              <div className="w-90 h-155 bg-background border border-foreground/10 rounded-2xl flex items-center justify-center text-foreground/40">
                <p>ChatPanel (구현 예정)</p>
              </div>
            </div>

            {/* Right: Next Button */}
            <div className="flex items-center self-center">
              <NavigationButton
                isLastQuestion={isLastQuestion}
                submitted={submitted}
                onNext={handleNext}
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
