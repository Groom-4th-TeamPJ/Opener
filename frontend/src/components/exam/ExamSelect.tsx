'use client'

import { useState } from 'react'
import Button from '@/components/common/Button'
import YearSelectBox from './YearSelectBox'
import QuestionSolve from './QuestionSolveView'
import { mockExamData } from '@/mocks/exam-data'
import type { ExamResponse } from '@/types/exam'

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

const currentYear = new Date().getFullYear() - 1

export default function ExamSelect() {
  const [selectedCategory, setSelectedCategory] = useState<string>('')
  const [selectedYear, setSelectedYear] = useState<number>(currentYear)
  const [selectedExamType, setSelectedExamType] = useState<string>('')
  const [examData, setExamData] = useState<ExamResponse | null>(null)

  const canStart = !!(selectedCategory && selectedYear && selectedExamType)

  const handleStartExam = () => {
    if (!canStart) return

    // const params = {
    //   year: Number(selectedYear),
    //   category: selectedCategory,
    //   examType: selectedExamType,
    // }

    // TODO: GET /api/exam?year={year}&category={category}&examType={examType}
    // TODO: Authorization: Bearer {accessToken} 헤더 추가

    // Mock 데이터 로드
    setExamData(mockExamData)
  }

  // 문제 풀이 화면으로 전환
  if (examData) {
    return (
      <div className="fixed inset-0 z-100 bg-background">
        <QuestionSolve data={examData} onClose={() => setExamData(null)} />
      </div>
    )
  }

  return (
    <div className="fixed inset-0 top-16 flex items-center justify-center">
      <div className="w-full max-w-6xl px-4">
        <div className="mb-10">
          <h1 className="text-[1.75rem] font-bold text-text-primary mb-2">
            어떤 문제부터 풀어볼까요?
          </h1>
          <p className="text-md text-text-secondary">과목과 시험을 고르면 바로 시작할 수 있어요</p>
        </div>

        <div className="bg-neutral-50 rounded-[1.25rem] p-8 mb-10">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            {/* 선택 영역 */}
            <div>
              <h2 className="text-xl text-center font-bold text-text-primary mb-10">선택 영역</h2>
              <div className="space-y-4">
                {categories.map((category) => (
                  <Button
                    key={category.id}
                    variant="ghost"
                    widthFull
                    className={`h-20 bg-white font-medium text-text-secondary ${
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
            <div>
              <h2 className="text-xl text-center font-bold text-text-primary mb-10">시험 연도</h2>
              <YearSelectBox
                value={selectedYear}
                onChange={setSelectedYear}
                isSelected={canStart}
              />
            </div>

            {/* 시험 종류 */}
            <div>
              <h2 className="text-xl text-center font-bold text-text-primary mb-10">시험 종류</h2>
              <div className="space-y-4">
                {examTypes.map((type) => (
                  <Button
                    key={type.id}
                    variant="ghost"
                    widthFull
                    className={`h-20 bg-white font-medium text-text-secondary ${
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
            className="font-bold h-12 p-4"
          >
            문제 풀이 시작하기
          </Button>
        </div>
      </div>
    </div>
  )
}
