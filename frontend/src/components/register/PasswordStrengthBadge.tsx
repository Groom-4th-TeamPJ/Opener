import { getPasswordStrength } from '@/utils/get-password-strength'
import { Badge } from '../common/Badge'

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

export default function PasswordStrengthBadge({ value }: { value: string }) {
  const strength = getPasswordStrength(value)
  return (
    <Badge
      type="solid-pastel"
      variant={PASSWORD_STRENGTH_META[strength].variant}
      size="sm"
      label={PASSWORD_STRENGTH_META[strength].label}
    />
  )
}
