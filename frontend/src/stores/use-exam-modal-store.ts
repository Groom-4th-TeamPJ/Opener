import { EXAM_MODAL } from '@/constants/exam'
import { create } from 'zustand'

// 관리할 모달들의 타입 정의
export type ExamModalType = (typeof EXAM_MODAL)[keyof typeof EXAM_MODAL] | null

interface ExamModalState {
  activeModal: ExamModalType
  // 특정 모달 열기
  openModal: (type: NonNullable<ExamModalType>) => void
  // 현재 떠 있는 모달 닫기
  closeModal: () => void
  // 특정 모달 열려있는지 여부
  isOpen: (type: Exclude<ExamModalType, null>) => boolean
}

export const useExamModalStore = create<ExamModalState>((set, get) => ({
  activeModal: null,
  openModal: (type) => set({ activeModal: type }),
  closeModal: () => set({ activeModal: null }),
  isOpen: (type) => {
    return get().activeModal === type
  },
}))
