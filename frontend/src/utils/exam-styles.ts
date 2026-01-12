interface ChoiceStyleParams {
  index: number
  selectedChoice: number | null
  submitted: boolean
  correctAnswer: number
  isCorrect: boolean | null
}

export const getChoiceStyle = ({
  index,
  selectedChoice,
  submitted,
  correctAnswer,
  isCorrect,
}: ChoiceStyleParams): string => {
  if (!submitted) {
    return selectedChoice === index
      ? 'border-primary-600 bg-primary-600/10'
      : 'border-foreground/20 hover:border-primary-600/30 hover:bg-foreground/5'
  }

  if (index === correctAnswer) {
    return 'border-green-600 bg-green-600/10 text-green-700'
  }
  if (index === selectedChoice && !isCorrect) {
    return 'border-red-600 bg-red-600/10 text-red-700'
  }
  return 'border-foreground/20 opacity-50'
}
