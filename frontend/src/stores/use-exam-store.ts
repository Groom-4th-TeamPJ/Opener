import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { ExamRequestParams } from '@/types/exam'

interface QuestionState {
  selectedChoice: number | null
  frqAnswer: string
  isSubmitted: boolean
  isCorrect: boolean | null
  correctAnswer: number | null
  isAnalysisActive: boolean
  hasNewQuestion: boolean
}

interface ExamState {
  // 시험 요청 파라미터
  examParams: ExamRequestParams | null
  // 시험 식별 정보
  examResultId: number | null
  // 현재 문제 인덱스
  currentIndex: number
  // 문제별 상태 (questionId를 키로)
  questionStates: Record<number, QuestionState>
  // 시험 시작 호출
  setExam: (params: ExamRequestParams, examResultId: number) => void
  // 다음 문제 이동
  goNextQuestion: () => void
  // 특정 문제 상태 업데이트
  updateQuestionState: (questionId: number, updates: Partial<QuestionState>) => void
  // 특정 문제 상태 가져오기
  getQuestionState: (questionId: number) => QuestionState
  // 전체 초기화
  resetExam: () => void
}

const initialQuestionState: QuestionState = {
  selectedChoice: null,
  frqAnswer: '',
  isSubmitted: false,
  isCorrect: null,
  correctAnswer: null,
  isAnalysisActive: false,
  hasNewQuestion: false,
}

export const useExamStore = create<ExamState>()(
  persist(
    (set, get) => ({
      examParams: null,
      examResultId: null,
      currentIndex: 0,
      questionStates: {},

      setExam: (params, examResultId) =>
        set((state) => {
          // 같은 시험이면 상태 유지
          if (state.examResultId === examResultId) {
            return state
          }

          // 새로운 시험이면 전체 초기화
          return {
            examParams: params,
            examResultId,
            currentIndex: 0,
            questionStates: {},
          }
        }),

      goNextQuestion: () =>
        set((state) => ({
          currentIndex: state.currentIndex + 1,
        })),

      updateQuestionState: (questionId, updates) =>
        set((state) => ({
          questionStates: {
            ...state.questionStates,
            [questionId]: {
              ...initialQuestionState,
              ...state.questionStates[questionId],
              ...updates,
            },
          },
        })),

      getQuestionState: (questionId) =>
        get().questionStates[questionId] ?? { ...initialQuestionState },

      resetExam: () =>
        set({
          examParams: null,
          examResultId: null,
          currentIndex: 0,
          questionStates: {},
        }),
    }),
    {
      name: 'exam-storage',
      partialize: (state) => ({
        examParams: state.examParams,
        examResultId: state.examResultId,
        currentIndex: state.currentIndex,
        questionStates: state.questionStates,
      }),
    }
  )
)
