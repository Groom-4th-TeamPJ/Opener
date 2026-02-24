type PasswordStrength = 'weak' | 'medium' | 'strong'

export function getPasswordStrength(password: string): PasswordStrength {
  const length = password.length

  const hasLetter = /[a-zA-Z]/.test(password)
  const hasNumber = /\d/.test(password)
  const hasSpecial = /[^a-zA-Z0-9]/.test(password)

  const isValidComposition = hasLetter && hasNumber && hasSpecial
  const isValidLength = length >= 8 && length <= 20

  if (!isValidComposition || !isValidLength) {
    return 'weak'
  }

  if (length >= 10) {
    return 'strong'
  }

  return 'medium' // 8~9자
}
