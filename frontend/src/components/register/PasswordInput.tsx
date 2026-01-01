'use client'

import { UseFormRegister, UseFormWatch } from 'react-hook-form'
import { RegisterFormValues } from '@/components/register/RegisterForm'
import Input from '@/components/common/Input'
import { useState } from 'react'
import { getPasswordStrength } from '@/utils/get-password-strength'
import { Eye, EyeOff } from 'lucide-react'
import cn from '@/utils/cn'

interface PasswordInputProps {
  register: UseFormRegister<RegisterFormValues>
  watch: UseFormWatch<RegisterFormValues>
  error?: string
}

const PASSWORD_STRENGTH_META = {
  weak: {
    label: '취약',
    text: 'text-red-600',
    border: 'border-red-600',
  },
  medium: {
    label: '적정',
    text: 'text-orange-500',
    border: 'border-orange-500',
  },
  strong: {
    label: '강력',
    text: 'text-green-600',
    border: 'border-green-600',
  },
} as const

export default function PasswordInput({ register, watch, error }: PasswordInputProps) {
  const password = watch('password')
  const [showPassword, setShowPassword] = useState<boolean>(false)
  const strength = getPasswordStrength(password)
  return (
    <div className="relative">
      <Input
        id="password"
        type={showPassword ? 'text' : 'password'}
        label="비밀번호"
        placeholder="•••••••••"
        helperText="영문, 숫자, 특수문자 (~!@#$%^&*) 조합 8~20 자리"
        error={error}
        {...register('password')}
      />
      {password.length > 0 && (
        <span
          className={cn(
            'absolute right-8 top-1/2 -translate-y-1/2 border rounded-xl text-xs mt-0.5 px-2 py-1',
            PASSWORD_STRENGTH_META[strength].text,
            PASSWORD_STRENGTH_META[strength].border
          )}
        >
          {PASSWORD_STRENGTH_META[strength].label}
        </span>
      )}
      <button
        type="button"
        onClick={() => setShowPassword((prev) => !prev)}
        className="absolute right-2 top-1/2 -translate-y-1/2 cursor-pointer text-foreground/65 pt-1"
      >
        {showPassword ? <EyeOff className="size-5" /> : <Eye className="size-5" />}
      </button>
    </div>
  )
}
