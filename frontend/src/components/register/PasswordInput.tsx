'use client'

import { UseFormRegister, UseFormWatch } from 'react-hook-form'
import { RegisterFormValues } from '@/components/register/RegisterForm'
import Input from '@/components/common/Input'
import { useState } from 'react'
import { getPasswordStrength } from '@/utils/get-password-strength'
import { Eye, EyeOff } from 'lucide-react'
import { Badge } from '@/components/common/Badge'
import Button from '@/components/common/Button'

interface PasswordInputProps {
  register: UseFormRegister<RegisterFormValues>
  watch: UseFormWatch<RegisterFormValues>
  error?: string
}

const PASSWORD_STRENGTH_META = {
  weak: {
    label: '취약',
    variant: 'danger',
  },
  medium: {
    label: '적정',
    variant: 'warning',
  },
  strong: {
    label: '강력',
    variant: 'success',
  },
} as const

const MAX_PASSWORD_LENGTH = 20

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
        maxLength={MAX_PASSWORD_LENGTH}
        rightIcon={
          <div className="absolute flex items-center gap-x-1.5 right-2">
            {password.length > 0 && (
              <Badge
                type="outline"
                variant={PASSWORD_STRENGTH_META[strength].variant}
                size="sm"
                pill
                label={PASSWORD_STRENGTH_META[strength].label}
              />
            )}
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={() => setShowPassword((prev) => !prev)}
              className="px-1"
            >
              {showPassword ? <EyeOff className="size-5" /> : <Eye className="size-5" />}
            </Button>
          </div>
        }
        {...register('password')}
      />
    </div>
  )
}
