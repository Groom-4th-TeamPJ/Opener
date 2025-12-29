'use client'

import { ComponentPropsWithRef, ReactNode, useEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { X } from 'lucide-react'
import cn from '@/utils/cn'

interface ModalProps extends ComponentPropsWithRef<'div'> {
  open: boolean
  onClose: () => void
  children?: ReactNode
  size?: 'sm' | 'md' | 'lg' | 'xl' | '2xl' | '3xl' | 'full'
  closeOnBackdrop?: boolean
  closeOnEscape?: boolean
  zIndex?: number
}

export function Modal({
  open,
  onClose,
  children,
  size = 'md',
  closeOnBackdrop = false,
  closeOnEscape = true,
  zIndex = 50, // 기본 zIndex 값 설정 (일반 모달은 50)
  className,
  ...props
}: ModalProps) {
  const modalRef = useRef<HTMLDivElement>(null)
  const scrollYRef = useRef<number>(0)
  const [mounted, setMounted] = useState(false)

  useEffect(() => {
    setMounted(true)
  }, [])

  useEffect(() => {
    if (!open || !closeOnEscape) return

    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose()
      }
    }

    document.addEventListener('keydown', handleEscape)
    return () => document.removeEventListener('keydown', handleEscape)
  }, [open, onClose, closeOnEscape])

  useEffect(() => {
    if (open) {
      scrollYRef.current = window.scrollY
      document.body.style.position = 'fixed'
      document.body.style.top = `-${scrollYRef.current}px`
      document.body.style.width = '100%'

      return () => {
        const scrollY = scrollYRef.current
        document.body.style.position = ''
        document.body.style.top = ''
        document.body.style.width = ''
        window.scrollTo(0, scrollY)
      }
    }
  }, [open])

  if (!open || !mounted) return null

  const handleBackdropClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (closeOnBackdrop && e.target === e.currentTarget) {
      onClose()
    }
  }

  const sizeClasses = {
    sm: 'max-w-sm',
    md: 'max-w-md',
    lg: 'max-w-lg',
    xl: 'max-w-xl',
    '2xl': 'max-w-2xl',
    '3xl': 'max-w-3xl',
    full: 'max-w-full mx-4',
  }

  const modalContent = (
    <>
      {/* Backdrop */}
      <div
        style={{ zIndex: zIndex }}
        className="fixed inset-0 bg-black/80 backdrop-blur-sm"
        onClick={handleBackdropClick}
      />

      {/* Content Wrapper */}
      <div
        style={{ zIndex: zIndex + 10 }}
        className="fixed inset-0 flex items-center justify-center p-4 pointer-events-none"
        role="dialog"
        aria-modal="true"
      >
        {/* Modal Content */}
        <div
          ref={modalRef}
          className={cn(
            'relative w-full max-h-[90vh] overflow-auto pointer-events-auto',
            'bg-background rounded-2xl border border-border shadow-lg',
            sizeClasses[size],
            className
          )}
          {...props}
        >
          {children}
        </div>
      </div>
    </>
  )

  return createPortal(modalContent, document.body)
}

// ModalHeader - 제목과 닫기 버튼을 포함하는 헤더
interface ModalHeaderProps extends ComponentPropsWithRef<'div'> {
  children?: ReactNode
  closable?: boolean
  onClose?: () => void
}

export function ModalHeader({
  className,
  children,
  closable = false,
  onClose,
  ...props
}: ModalHeaderProps) {
  return (
    <div className={cn('flex items-start justify-between p-6', className)} {...props}>
      <div className="flex-1 text-foreground">{children}</div>
      {closable && onClose && (
        <button
          onClick={onClose}
          className="ml-4 p-1 rounded-lg hover:bg-muted transition-colors cursor-pointer"
          aria-label="닫기"
        >
          <X className="size-4" />
        </button>
      )}
    </div>
  )
}

// ModalContent - 주요 컨텐츠 영역
interface ModalContentProps extends ComponentPropsWithRef<'div'> {
  children?: ReactNode
}

export function ModalContent({ className, children, ...props }: ModalContentProps) {
  return (
    <div className={cn('p-6 text-foreground', className)} {...props}>
      {children}
    </div>
  )
}

// ModalFooter - 액션 버튼들을 포함하는 푸터
interface ModalFooterProps extends ComponentPropsWithRef<'div'> {
  children?: ReactNode
}

export function ModalFooter({ className, children, ...props }: ModalFooterProps) {
  return (
    <div className={cn('flex items-center justify-end gap-3 p-6', className)} {...props}>
      {children}
    </div>
  )
}
