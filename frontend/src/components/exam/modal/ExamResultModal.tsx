'use client'
import Button from '@/components/common/Button'
import { Modal, ModalContent, ModalFooter, ModalHeader } from '@/components/common/Modal'
import { useRouter } from 'next/navigation'
import { memo, useCallback } from 'react'
import ExamResultSummary from '@/components/exam/ExamResultSummary'

interface ExamResultModalProps {
  open: boolean
  onClose: () => void
}

export default memo(function ExamResultModal({ open, onClose }: ExamResultModalProps) {
  const router = useRouter()

  const handleGoToDashboard = useCallback(() => {
    router.replace('/')
  }, [router])

  return (
    <Modal
      open={open}
      onClose={onClose}
      closeOnBackdrop={false}
      closeOnEscape={false}
      zIndex={100}
      className="md:max-w-97.5 lg:max-w-150"
    >
      <ModalHeader onClose={onClose} className="flex">
        <div className="flex justify-between">
          <h2 className="font-bold md:text-xl lg:text-2xl">학습 결과</h2>
        </div>
      </ModalHeader>

      <ModalContent className="flex flex-col gap-4">
        <ExamResultSummary />
      </ModalContent>
      <ModalFooter>
        <Button className="flex-1 h-12" onClick={handleGoToDashboard}>
          대시보드로 이동하기
        </Button>
      </ModalFooter>
    </Modal>
  )
})
