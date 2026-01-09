import Image from 'next/image'
import Button from '../common/Button'
import { Modal, ModalContent, ModalFooter } from '../common/Modal'

interface ExamExitModalProps {
  open: boolean
  onCancel: () => void
  onConfirm: () => void
}

export default function ExamExitModal({ open, onCancel, onConfirm }: ExamExitModalProps) {
  return (
    <Modal
      open={open}
      onClose={onCancel}
      closeOnBackdrop={false}
      closeOnEscape={false}
      zIndex={100}
      className="w-full md:w-100 lg:w-122"
    >
      <ModalContent>
        <div className="py-5 lg:py-7 flex flex-col justify-center items-center gap-5">
          {/* 아이콘 영역 */}
          <div className="w-15 h-15 lg:w-20 lg:h-20 relative">
            <Image src="/icons/info-fill_gray.svg" alt="" fill className="object-contain" />
          </div>

          {/* 텍스트 영역 */}
          <div className="flex flex-col justify-start items-center gap-3">
            <h2 className="text-center text-text-primary text-xl md:text-2xl lg:text-3xl font-bold leading-relaxed">
              잠깐만요!
              <br />
              아직 학습이 끝나지 않았어요
            </h2>
            <p className="text-center text-text-primary text-sm md:text-base font-normal leading-6">
              지금 나가면 풀이 데이터가 저장되지 않아요
            </p>
          </div>
        </div>
      </ModalContent>

      <ModalFooter className="flex-col">
        <Button
          variant="default"
          size="lg"
          widthFull
          onClick={onCancel}
          className="text-base font-medium"
        >
          계속 학습하기
        </Button>
        <Button
          variant="secondary"
          size="lg"
          widthFull
          onClick={onConfirm}
          className="text-base font-medium"
        >
          학습 종료하기
        </Button>
      </ModalFooter>
    </Modal>
  )
}
