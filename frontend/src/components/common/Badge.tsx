import cn from '@/utils/cn'
import { CSSProperties, HTMLAttributes, ReactNode } from 'react'

type BadgeType = 'outline' | 'solid' | 'solid-pastel'
type BadgeVariant = 'primary' | 'secondary' | 'warning' | 'danger' | 'success' | 'info'
type BadgeSize = 'sm' | 'md' | 'lg'

interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
  type?: BadgeType
  variant?: BadgeVariant
  size?: BadgeSize
  pill?: boolean
  label: string
  leftIcon?: ReactNode
}

const base: string =
  'inline-flex items-center justify-center gap-1 whitespace-nowrap select-none ' +
  'ring-1 ring-inset transition-colors'

const sizeClass: Record<BadgeSize, string> = {
  sm: 'h-5 px-2 text-xs',
  md: 'h-6 px-3 text-sm leading-0',
  lg: 'h-7 px-4 text-base',
}

// 각 color마다 base/pastel/fg를 제공

const COLOR_TOKENS: Record<BadgeVariant, { base: string; pastel: string; fgOnBase: string }> = {
  primary: {
    base: 'var(--color-primary-600)',
    pastel: 'var(--color-primary-100)',
    fgOnBase: 'var(--color-white)',
  },
  secondary: {
    base: 'var(--color-neutral-600)',
    pastel: 'var(--color-neutral-100)',
    fgOnBase: 'var(--color-white)',
  },
  warning: {
    base: 'var(--color-warning-600)',
    pastel: 'var(--color-warning-100)',
    fgOnBase: 'var(--color-white)',
  },
  danger: {
    base: 'var(--color-danger-600)',
    pastel: 'var(--color-danger-100)',
    fgOnBase: 'var(--color-white)',
  },
  success: {
    base: 'var(--color-success-600)',
    pastel: 'var(--color-success-100)',
    fgOnBase: 'var(--color-white)',
  },
  info: {
    base: 'var(--color-info-600)',
    pastel: 'var(--color-info-100)',
    fgOnBase: 'var(--color-white)',
  },
}

/**
 * type은 방식만 결정,
 * COLOR_TOKENS에서 꺼낸 토큰들을 조합해서 스타일 결정
 */
function typeStyle(type: BadgeType, t: (typeof COLOR_TOKENS)[BadgeVariant]) {
  switch (type) {
    case 'outline':
      return { '--badge-bg': 'transparent', '--badge-text': t.base, '--badge-border': t.base }

    case 'solid':
      return { '--badge-bg': t.base, '--badge-text': t.fgOnBase, '--badge-border': 'transparent' }

    case 'solid-pastel':
      return { '--badge-bg': t.pastel, '--badge-text': t.base, '--badge-border': 'transparent' }
  }
}

export function Badge({
  className,
  type = 'solid',
  variant = 'primary',
  size = 'md',
  pill = false,
  label,
  leftIcon,
  ...props
}: BadgeProps) {
  const t: (typeof COLOR_TOKENS)[BadgeVariant] = COLOR_TOKENS[variant]

  return (
    <span
      style={typeStyle(type, t) as CSSProperties}
      className={cn(
        base,
        sizeClass[size],
        'bg-(--badge-bg) text-(--badge-text) ring-(--badge-border)',
        pill ? 'rounded-full' : 'rounded-md',
        className
      )}
      {...props}
    >
      {leftIcon && <span className="shrink-0">{leftIcon}</span>}
      {label}
    </span>
  )
}
