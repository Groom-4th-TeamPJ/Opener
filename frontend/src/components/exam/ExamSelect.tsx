'use client'

import { useState } from 'react'
import Button from '@/components/common/Button'
import YearSelectBox from './YearSelectBox'
import QuestionSolveView from './QuestionSolveView'
import { useStartExam } from '@/hooks/exam/use-start-exam'
import ExamLoading from './ExamLoading'
import { toast } from 'sonner'

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

  const canStart = !!(selectedCategory && selectedYear && selectedExamType)

  const { mutateAsync, isPending } = useStartExam()

  const handleStartExam = async () => {
    if (!canStart) return

    try {
      const result = await mutateAsync({
        examYear: selectedYear,
        category: selectedCategory,
        examType: selectedExamType,
      })

      if (result) {
        setIsExamActive(true)
      }
    } catch (error) {
      console.error('[ERROR] 문제 풀이 요청', error)
      toast.error('문제를 불러오지 못했습니다. 다시 시도해주세요.', { duration: 3000 })
    }
  }

  const handleClose = () => {
    setIsExamActive(false)
  }

  // 문제 풀이 요청 로딩
  if (isPending) {
    return <ExamLoading />
  }

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
