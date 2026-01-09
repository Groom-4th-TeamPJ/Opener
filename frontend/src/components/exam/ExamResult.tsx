'use client'
import Button from '@/components/common/Button'
import { Modal, ModalContent, ModalFooter, ModalHeader } from '@/components/common/Modal'
import { useRouter } from 'next/navigation'
import ExamResultSummary from './ExamResultSummary'

export default function ExamResult() {
  const router = useRouter()

  const onClose = () => {
    router.back()
  }

  const handleGoToDashboard = () => {
    router.push('/')
  }
  return (
    <Modal open={true} onClose={onClose} className="md:max-w-97.5 lg:max-w-150">
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
}
