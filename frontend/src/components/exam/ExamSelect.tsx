'use client'

import { useState } from 'react'
import Button from '@/components/common/Button'
import YearSelectBox from './YearSelectBox'
import QuestionSolveView from './QuestionSolveView'
import { useStartExam } from '@/hooks/exam/use-start-exam'
import { useSSEChat } from '@/hooks/exam/use-sse-chat'

const categories = [
  { id: 'CALC', name: '미적분' },
  { id: 'PROB', name: '확률과 통계' },
  { id: 'GEO', name: '기하' },
]

const examTypes = [
  { id: 'M06', name: '6월 모의평가' },
  { id: 'M09', name: '9월 모의평가' },
  { id: 'CSAT', name: '수학능력시험' },
]

const currentYear = new Date().getFullYear()

export default function ExamSelect() {
  const [selectedCategory, setSelectedCategory] = useState<string>('')
  const [selectedYear, setSelectedYear] = useState<number>(currentYear)
  const [selectedExamType, setSelectedExamType] = useState<string>('')
  const [isExamActive, setIsExamActive] = useState(false)
  const [examResultId, setExamResultId] = useState<number | null>(null)

  const canStart = !!(selectedCategory && selectedYear && selectedExamType)

  const { mutateAsync } = useStartExam()

  // SSE 연결 (examResultId가 있을 때만 연결)
  useSSEChat({ sessionId: examResultId ?? 0, enabled: !!examResultId })

  const handleStartExam = async () => {
    if (!canStart) return

    const result = await mutateAsync({
      examYear: selectedYear,
      category: selectedCategory,
      examType: selectedExamType,
    })

    if (result) {
      setExamResultId(result.examResultId)
      setIsExamActive(true)
    }
  }

  const handleClose = () => {
    setIsExamActive(false)
  }
  //TODO: 새로고침 문제 복원 시 examResultId localStorage 저장 필요 & 조건문 변경
  // 문제 풀이 화면으로 전환
  if (isExamActive) {
    return (
      <div className="fixed inset-0 z-100 bg-background">
        <QuestionSolveView
          params={{
            examYear: selectedYear,
            category: selectedCategory,
            examType: selectedExamType,
          }}
          onClose={handleClose}
        />
      </div>
    )
  }

  return (
    <div className="fixed inset-0 top-16 overflow-y-auto">
      <div className="min-h-full flex items-center justify-center py-8">
        <div className="w-full max-w-6xl px-4">
          <div className="mb-10">
            <h1 className="text-[1.75rem] font-bold text-text-primary mb-2">
              어떤 문제부터 풀어볼까요?
            </h1>
            <p className="text-text-secondary">과목과 시험을 고르면 바로 시작할 수 있어요</p>
          </div>

          <div className="bg-neutral-50 rounded-20 mb-10">
            <div className="grid grid-cols-1 md:grid-cols-3 gap-8 md:gap-0">
              {/* 선택 영역 */}
              <div className="p-8 md:border-r md:border-neutral-100">
                <h2 className="text-lg lg:text-xl text-center font-bold text-text-primary mb-10">
                  선택 영역
                </h2>
                <div className="space-y-4">
                  {categories.map((category) => (
                    <Button
                      key={category.id}
                      variant="ghost"
                      widthFull
                      className={`h-15 lg:h-20 bg-white font-medium text-text-secondary ${
                        selectedCategory === category.id
                          ? 'font-bold  text-text-primary border-2 border-primary-600 hover:bg-white'
                          : 'hover:bg-neutral-100'
                      }`}
                      onClick={() => setSelectedCategory(category.id)}
                    >
                      {category.name}
                    </Button>
                  ))}
                </div>
              </div>

              {/* 시험 연도 */}
              <div className="p-8 md:border-r md:border-neutral-100">
                <h2 className="text-lg lg:text-xl text-center font-bold text-text-primary mb-10">
                  시험 연도
                </h2>
                <YearSelectBox
                  value={selectedYear}
                  onChange={setSelectedYear}
                  isSelected={canStart}
                />
              </div>

              {/* 시험 종류 */}
              <div className="p-8">
                <h2 className="text-lg lg:text-xl text-center font-bold text-text-primary mb-10">
                  시험 종류
                </h2>
                <div className="space-y-4">
                  {examTypes.map((type) => (
                    <Button
                      key={type.id}
                      variant="ghost"
                      widthFull
                      className={`h-15 lg:h-20 bg-white font-medium text-text-secondary ${
                        selectedExamType === type.id
                          ? 'font-bold text-text-primary border-2 border-primary-600 hover:bg-white'
                          : 'hover:bg-neutral-100'
                      }`}
                      onClick={() => setSelectedExamType(type.id)}
                    >
                      {type.name}
                    </Button>
                  ))}
                </div>
              </div>
            </div>
          </div>

          <div className="flex justify-end">
            <Button
              variant="default"
              size="lg"
              disabled={!canStart}
              onClick={handleStartExam}
              className="font-bold p-4"
            >
              문제 풀이 시작하기
            </Button>
          </div>
        </div>
      </div>
    </div>
  )
}
