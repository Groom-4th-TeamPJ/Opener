'use client'

import { useState } from 'react'
import { ArrowRight } from 'lucide-react'
import Button from '@/components/common/Button'
import SelectBox from '@/components/common/SelectBox'

const subjects = [
  { id: 'calculus', name: '미적분' },
  { id: 'probability', name: '확률과 통계' },
  { id: 'geometry', name: '기하' },
]

const examTypes = [
  { id: 'june', name: '6월 모평' },
  { id: 'september', name: '9월 모평' },
  { id: 'suneung', name: '수능' },
]

const yearOptions = Array.from({ length: 5 }, (_, i) => ({
  value: String(2025 - i),
  label: String(2025 - i),
}))

export default function ExamSelect() {
  const [selectedSubject, setSelectedSubject] = useState<string>('')
  const [selectedYear, setSelectedYear] = useState<string>(yearOptions[0].value)
  const [selectedExamType, setSelectedExamType] = useState<string>('')

  const canStart = selectedSubject && selectedYear && selectedExamType

  // TODO: 문제 풀이 시작 기능 구현

  return (
    <>
      <div className="mb-10">
        <h1 className="text-2xl font-semibold text-foreground mb-2">
          풀고 싶은 과목과 시험을 선택하세요.
        </h1>
        <p className="text-lg text-foreground/60">
          선택이 끝나면 바로 문제 풀이를 시작할 수 있어요.
        </p>
      </div>

      <div className="bg-foreground/5 rounded-2xl p-8 mb-8">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          {/* 선택 영역 */}
          <div>
            <h3 className="text-center font-semibold text-foreground/70 mb-6">선택 영역</h3>
            <div className="space-y-3">
              {subjects.map((subject) => (
                <Button
                  key={subject.id}
                  variant="ghost"
                  widthFull
                  className={
                    selectedSubject === subject.id
                      ? 'h-16 bg-primary-600/10 border border-primary-600 hover:bg-primary-600/10'
                      : 'h-16 bg-background border border-foreground/20'
                  }
                  onClick={() => setSelectedSubject(subject.id)}
                >
                  {subject.name}
                </Button>
              ))}
            </div>
          </div>

          {/* 시험 연도 */}
          <div>
            <h3 className="text-center font-semibold text-foreground/70 mb-6">시험 연도</h3>
            <SelectBox
              options={yearOptions}
              value={selectedYear}
              onChange={setSelectedYear}
              placeholder="연도 선택"
              triggerClassName="h-16"
            />
          </div>

          {/* 시험 종류 */}
          <div>
            <h3 className="text-center font-semibold text-foreground/70 mb-6">시험 종류</h3>
            <div className="space-y-3">
              {examTypes.map((type) => (
                <Button
                  key={type.id}
                  variant="ghost"
                  widthFull
                  className={`h-16 ${
                    selectedExamType === type.id
                      ? 'bg-primary-600/10 border border-primary-600 hover:bg-primary-600/10'
                      : 'bg-background border border-foreground/20'
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
          rightIcon={<ArrowRight className="w-5 h-5 -mr-2" />}
          className="font-semibold"
        >
          문제 풀이 시작하기
        </Button>
      </div>
    </>
  )
}
