import { useEffect } from 'react'

interface UsePreventRefreshProps {
  enabled: boolean
  onPrevent?: () => void
}

export default function usePreventRefresh({ enabled, onPrevent }: UsePreventRefreshProps) {
  // 1. 키보드 새로고침 방지
  useEffect(() => {
    if (!enabled) return

    const handleKeyDown = (e: KeyboardEvent) => {
      const isRefresh =
        e.key === 'F5' ||
        (e.ctrlKey && e.key.toLowerCase() === 'r') ||
        (e.metaKey && e.key.toLowerCase() === 'r')

      if (isRefresh) {
        e.preventDefault()
        onPrevent?.()
      }
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => {
      window.removeEventListener('keydown', handleKeyDown)
    }
  }, [enabled, onPrevent])

  // 2. 브라우저 새로고침 / 닫기 방지
  useEffect(() => {
    if (!enabled) return

    const handleBeforeUnload = (e: BeforeUnloadEvent) => {
      // 표준에 따라 preventDefault 호출 및 returnValue 설정
      e.preventDefault()
      e.returnValue = '' // 크롬 등 대부분의 브라우저에서 경고창을 띄우게 함
    }

    // 브라우저 새로고침, 닫기, 뒤로가기(일부) 등 방지
    window.addEventListener('beforeunload', handleBeforeUnload)

    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload)
    }
  }, [enabled])
}
