'use client'

import { useEffect, useMemo, useState } from 'react'

type CircularChartProps = {
  percent: number // 0~100
  label?: string
  size?: number
  strokeWidth?: number
  durationMs?: number
}

// 1 ~ 100까지의 수로 자르기
function clamp01to100(n: number) {
  return Math.min(100, Math.max(0, n))
}

function easeOutCubic(t: number) {
  return 1 - Math.pow(1 - t, 3)
}

function usePrefersReducedMotion() {
  const [reduced, setReduced] = useState(false)

  useEffect(() => {
    const mql = window.matchMedia?.('(prefers-reduced-motion: reduce)')
    if (!mql) return
    const onChange = () => setReduced(mql.matches)
    onChange()
    mql.addEventListener?.('change', onChange)
    return () => mql.removeEventListener?.('change', onChange)
  }, [])

  return reduced
}

export default function CircularChart({
  percent,
  label = 'N문제',
  size = 180,
  strokeWidth = 15,
  durationMs = 900,
}: CircularChartProps) {
  const target = clamp01to100(percent)
  const prefersReducedMotion = usePrefersReducedMotion()

  const radius = useMemo(() => (size - strokeWidth) / 2, [size, strokeWidth])
  const circumference = useMemo(() => 2 * Math.PI * radius, [radius])

  const [display, setDisplay] = useState(0) // 숫자 표시용(정수)
  const [progress, setProgress] = useState(0) // 원형 진행용(0~target, 소수)

  useEffect(() => {
    // 모션 줄이기 설정이면 정답률 즉시 반영
    if (prefersReducedMotion) {
      setDisplay(Math.round(target))
      setProgress(target)
      return
    }

    let raf = 0
    const start = performance.now()
    const from = 0
    const to = target

    const tick = (now: number) => {
      const elapsed = now - start
      const t = Math.min(1, elapsed / durationMs)
      const eased = easeOutCubic(t)

      const value = from + (to - from) * eased
      setProgress(value)
      setDisplay(Math.round(value))

      if (t < 1) raf = requestAnimationFrame(tick)
    }

    raf = requestAnimationFrame(tick)
    return () => cancelAnimationFrame(raf)
  }, [target, durationMs, prefersReducedMotion])

  const offset = circumference * (1 - progress / 100)

  return (
    <svg width={size} height={size} role="img" aria-label={`${label} ${display}%`}>
      <circle
        cx={size / 2}
        cy={size / 2}
        r={radius}
        fill="none"
        stroke="#d2d2d0"
        strokeWidth={strokeWidth}
      />

      <circle
        cx={size / 2}
        cy={size / 2}
        r={radius}
        fill="none"
        stroke="#00b3ab"
        strokeWidth={strokeWidth}
        strokeDasharray={circumference}
        strokeDashoffset={offset}
        transform={`rotate(-90 ${size / 2} ${size / 2})`}
      />

      <text x="50%" y="50%" textAnchor="middle" dominantBaseline="middle">
        <tspan x="50%" dy="-0.2em" fontSize="28" fontWeight="700" fill="#008c85">
          {display}%
        </tspan>
        <tspan x="50%" dy="2em" fontSize="14" fill="#7d7e7f">
          {label}
        </tspan>
      </text>
    </svg>
  )
}
