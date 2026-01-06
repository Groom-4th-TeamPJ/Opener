'use client'

import React, { useState, useRef, ComponentPropsWithoutRef } from 'react'
import { useController, Control, Path, FieldValues } from 'react-hook-form'
import Input from '@/components/common/Input'
import { Eye, EyeOff, X } from 'lucide-react'
import cn from '@/utils/cn'
import Button from '@/components/common/Button'

type InputProps = ComponentPropsWithoutRef<typeof Input>

interface AuthInputProps<T extends FieldValues> extends Omit<
  InputProps,
  'value' | 'onChange' | 'onBlur'
> {
  name: Path<T>
  control: Control<T>
  error?: string
  // 비밀번호 강도나 상태를 표시할 badge 렌더러
  badgeRenderer?: (value: string) => React.ReactNode | null
}

export default function AuthInput<T extends FieldValues>({
  name,
  control,
  type = 'text',
  maxLength,
  autoComplete,
  error,
  badgeRenderer,
  ...props
}: AuthInputProps<T>) {
  const [isFocused, setIsFocused] = useState(false)
  const [showPassword, setShowPassword] = useState(false)
  const inputRef = useRef<HTMLInputElement>(null)

  const {
    field: { onChange, onBlur, value, ref: controllerRef },
  } = useController({
    name,
    control,
  })

  // completed 상태 (focus out + trim 후 비어있지 않음)
  const isCompleted = !isFocused && typeof value === 'string' && value.trim() !== ''

  // Reset 로직: 값 초기화 및 포커스 유지
  const handleReset = (e: React.MouseEvent) => {
    e.preventDefault() // 버튼 클릭 시 포커스 방해 금지
    onChange('')
    setTimeout(() => inputRef.current?.focus(), 0)
  }

  // Eye 토글 로직: 비밀번호 토글 및 blur 방지
  const togglePassword = (e: React.MouseEvent) => {
    e.preventDefault() // 버튼 클릭 시 포커스 방해 금지
    setShowPassword((prev) => !prev)
  }

  // 포커스 이벤트 핸들러
  const handleFocus = () => setIsFocused(true)
  const handleBlur = () => {
    setIsFocused(false)
    onBlur()
  }

  // rightIcon 영역 렌더링 로직
  const renderRightIcon = () => {
    const isPassword = type === 'password'
    const hasValue = value && String(value).length > 0

    return (
      <div
        className="absolute flex items-center gap-x-1 right-2"
        onMouseDown={(e) => e.preventDefault()}
      >
        {/* Password 전용: Badge (항상 표시) */}
        {isPassword && badgeRenderer && badgeRenderer(value || '')}

        {/* Focus 상태일 때만 표시되는 제어 버튼들 */}
        {isFocused && (
          <div className="flex items-center gap-1">
            {/* Password 전용: Eye 버튼 */}
            {isPassword && (
              <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={togglePassword}
                className="px-1"
              >
                {showPassword ? <EyeOff className="size-5" /> : <Eye className="size-5" />}
              </Button>
            )}

            {/* 공통: Reset 버튼 */}
            {hasValue && (
              <Button
                type="button"
                variant="ghost"
                onClick={handleReset}
                className="p-1 hover:bg-transparent"
                tabIndex={-1}
              >
                <X
                  className="size-4 rounded-full bg-neutral-200 text-white p-0.5"
                  strokeWidth={4}
                />
              </Button>
            )}
          </div>
        )}
      </div>
    )
  }

  return (
    <Input
      {...props}
      ref={(e) => {
        controllerRef(e)
        inputRef.current = e
      }}
      type={type === 'password' && showPassword ? 'text' : type}
      value={value ?? ''}
      onChange={onChange}
      onFocus={handleFocus}
      onBlur={handleBlur}
      error={error}
      maxLength={maxLength}
      autoComplete={autoComplete}
      rightIcon={renderRightIcon()}
      size="lg"
      className={cn(props.className, !error && isCompleted && 'border-neutral-600')}
    />
  )
}
