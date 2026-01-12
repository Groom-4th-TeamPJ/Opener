'use client'

import Image from 'next/image'
import { Modal, ModalContent, ModalFooter } from '@/components/common/Modal'
import Button from '@/components/common/Button'

interface InactivityModalProps {
  open: boolean
  onConfirm: () => void
}

export default function InactivityModal({ open, onConfirm }: InactivityModalProps) {
  return (
    <Modal
      open={open}
      onClose={onConfirm}
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
              혹시
              <br />
              떠나셨나요..?
            </h2>
            <p className="text-center text-text-primary text-sm md:text-base font-normal leading-6">
              장시간 자리비움으로 인해 문제 풀이를 종료합니다.
            </p>
          </div>
        </div>
      </ModalContent>

      <ModalFooter className="flex-col">
        <Button
          variant="secondary"
          size="lg"
          widthFull
          onClick={onConfirm}
          className="text-base font-medium"
        >
          확인하기
        </Button>
      </ModalFooter>
    </Modal>
  )
}
