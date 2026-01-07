'use client'

import { useState, useEffect, useRef, useImperativeHandle } from 'react'
import { formatTime } from '@/utils/format'
import type { StopwatchProps } from '@/types/exam'
import TimerIcon from '@/components/icons/TimerIcon'

export default function Stopwatch({ onTimeChange, autoStart = true, ref }: StopwatchProps) {
  const [elapsedSeconds, setElapsedSeconds] = useState(0)
  const [isRunning, setIsRunning] = useState(autoStart)
  const timerRef = useRef<NodeJS.Timeout | null>(null)

  // 스탑워치 타이머
  useEffect(() => {
    if (!isRunning) return

    const intervalId = setInterval(() => {
      setElapsedSeconds((prev) => {
        const newSeconds = prev + 1
        onTimeChange?.(newSeconds)
        return newSeconds
      })
    }, 1000)

    timerRef.current = intervalId

    return () => {
      clearInterval(intervalId)
    }
  }, [isRunning, onTimeChange])

  // cleanup
  useEffect(() => {
    return () => {
      if (timerRef.current) {
        clearInterval(timerRef.current)
      }
    }
  }, [])

  // ref를 통해 외부에서 제어할 수 있는 메서드 노출
  useImperativeHandle(ref, () => ({
    start: () => {
      setIsRunning(true)
    },
    stop: () => {
      setIsRunning(false)
      if (timerRef.current) {
        clearInterval(timerRef.current)
      }
    },
    reset: () => {
      setElapsedSeconds(0)
      setIsRunning(autoStart)
    },
    getTime: () => {
      return elapsedSeconds
    },
  }))

  return (
    <div className="inline-flex items-center gap-2">
      <TimerIcon />
      <div className="text-text-primary text-lg leading-7 min-w-14 tabular-nums">
        {formatTime(elapsedSeconds)}
      </div>
    </div>
  )
}
