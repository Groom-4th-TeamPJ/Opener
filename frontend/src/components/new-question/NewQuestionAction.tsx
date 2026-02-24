import type { NewQuestionActionProps } from '@/types/exam-variant'
import Button from '@/components/common/Button'

export default function NewQuestionAction({
  type,
  isSubmitted,
  selected,
  frqAnswer,
  onSubmit,
  onBack,
}: NewQuestionActionProps) {
  const isValueEmpty = type === 'MCQ' ? selected == null : frqAnswer == null
  return (
    <>
      {!isSubmitted ? (
        <Button className="flex-1 h-12" disabled={isValueEmpty} onClick={onSubmit}>
          제출하기
        </Button>
      ) : (
        <Button className="flex-1 h-12" onClick={onBack}>
          원래 문제로 돌아가기
        </Button>
      )}
    </>
  )
}
