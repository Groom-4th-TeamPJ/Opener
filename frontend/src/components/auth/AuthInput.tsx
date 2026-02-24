'use client'

import React, { useState, useRef, ComponentPropsWithoutRef } from 'react'
import { useController, Control, Path, FieldValues } from 'react-hook-form'
import Input from '@/components/common/Input'
import cn from '@/utils/cn'
import Button from '@/components/common/Button'
import Image from 'next/image'

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
        {isPassword && badgeRenderer && badgeRenderer(value || '')}

        {isFocused && (
          <div className="flex items-center gap-1">
            {isPassword && (
              <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={togglePassword}
                className="px-1"
              >
                {showPassword ? (
                  <Image
                    src={'icons/eye_off-outline.svg'}
                    alt="비밀번호 숨기기"
                    width={20}
                    height={20}
                    className="shrink-0"
                  />
                ) : (
                  <Image
                    src={'icons/eye_outline.svg'}
                    alt="비밀번호 보기"
                    width={20}
                    height={20}
                    className="shrink-0"
                  />
                )}
              </Button>
            )}

            {hasValue && (
              <Button
                type="button"
                variant="ghost"
                onClick={handleReset}
                className="p-1 hover:bg-transparent"
                tabIndex={-1}
              >
                <Image
                  src={'icons/close_white.svg'}
                  alt="리셋"
                  width={16}
                  height={16}
                  className="shrink-0 rounded-full bg-neutral-200 text-white p-0.5"
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
      name={name}
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
      autoComplete={name}
      rightIcon={renderRightIcon()}
      size="lg"
      className={cn(props.className, !error && isCompleted && 'border-neutral-600')}
    />
  )
}
