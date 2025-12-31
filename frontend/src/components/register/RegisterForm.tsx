'use client'

import { useForm } from 'react-hook-form'
import RegisterFormView from '@/components/register/RegisterFormView'
import z from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'

const registerSchema = z.object({
  name: z.string(),
  email: z.email(),
  password: z.string(),
})

export type RegisterFormValues = z.infer<typeof registerSchema>

export default function LoginForm() {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    clearErrors,
    setError,
    resetField,
  } = useForm({
    defaultValues: { name: '', email: '', password: '' },
    resolver: zodResolver(registerSchema),
  })

  const onSubmit = async (form: RegisterFormValues) => {
    clearErrors()
    try {
      // TODO: 추후 API 연동
    } catch {
      resetField('password')
    }
  }

  return (
    <RegisterFormView
      register={register}
      onSubmit={handleSubmit(onSubmit)}
      errors={errors}
      isSubmitting={isSubmitting}
    />
  )
}
