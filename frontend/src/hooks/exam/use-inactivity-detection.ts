import { useEffect, useRef } from 'react'

interface UseInactivityDetectionProps {
  timeout: number
  onInactive: () => void
  enabled?: boolean
}

/**
 * 사용자 비활성 상태를 감지하는 커스텀 훅
 * @param timeout - 비활성 판정 시간 (밀리초)
 * @param onInactive - 비활성 상태가 되었을 때 실행할 콜백
 * @param enabled - 비활성 감지 활성화 여부 (기본값: true)
 */
export default function useInactivityDetection({
  timeout,
  onInactive,
  enabled = true,
}: UseInactivityDetectionProps) {
  const inactivityTimerRef = useRef<NodeJS.Timeout | null>(null)

  useEffect(() => {
    if (!enabled) return

    // 비활성 타이머 시작 함수
    const startInactivityTimer = () => {
      // 기존 타이머가 있으면 클리어
      if (inactivityTimerRef.current) {
        clearTimeout(inactivityTimerRef.current)
      }
      // timeout 후 콜백 실행
      inactivityTimerRef.current = setTimeout(() => {
        onInactive()
      }, timeout)
    }

    // 활동 감지 시 타이머 리셋
    const handleActivity = () => {
      startInactivityTimer()
    }

    // 초기 타이머 시작
    startInactivityTimer()

    // 이벤트 리스너 등록
    window.addEventListener('mousemove', handleActivity)
    window.addEventListener('mousedown', handleActivity)
    window.addEventListener('keydown', handleActivity)
    window.addEventListener('scroll', handleActivity)
    window.addEventListener('touchstart', handleActivity)

    return () => {
      // 클린업: 타이머와 이벤트 리스너 제거
      if (inactivityTimerRef.current) {
        clearTimeout(inactivityTimerRef.current)
      }
      window.removeEventListener('mousemove', handleActivity)
      window.removeEventListener('mousedown', handleActivity)
      window.removeEventListener('keydown', handleActivity)
      window.removeEventListener('scroll', handleActivity)
      window.removeEventListener('touchstart', handleActivity)
    }
  }, [timeout, onInactive, enabled])
}
