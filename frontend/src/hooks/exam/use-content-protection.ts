import type { MouseEvent, ClipboardEvent, DragEvent } from 'react'

// 모듈 로드시 함수 한 번만 생성
const handleContextMenu = (e: MouseEvent) => {
  e.preventDefault()
}

const handleCopy = (e: ClipboardEvent) => {
  e.preventDefault()
}

const handleDragStart = (e: DragEvent) => {
  e.preventDefault()
}

export default function useContentProtection() {
  return {
    handleContextMenu,
    handleCopy,
    handleDragStart,
  }
}
