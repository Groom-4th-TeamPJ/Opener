import { TermKey } from '@/types/auth.types'
import cn from '@/utils/cn'
import CircleCheckbox from './CircleCheckBox'
import Button from '@/components/common/Button'

interface TermItemProps {
  termKey: TermKey
  checked: boolean
  onChange: (next: boolean) => void
  label: string
  hasContent?: boolean
  onOpen?: (key: TermKey) => void
}

export default function TermItem({
  termKey,
  checked,
  onChange,
  label,
  hasContent,
  onOpen,
}: TermItemProps) {
  return (
    <div className="rounded-lg bg-background border border-foreground/20 p-3">
      <div className={cn('flex justify-between items-center gap-3 select-none')}>
        {/* 체크 + 라벨 */}
        <CircleCheckbox
          checked={checked}
          onChange={onChange}
          label={
            <>
              {label} <span className="text-danger-600">(필수)</span>
            </>
          }
        />

        {/* 전문 보기 */}
        {hasContent && (
          <Button
            type="button"
            variant="ghost"
            onClick={() => onOpen?.(termKey)}
            className={cn(
              'px-0 h-4 text-xs text-text-secondary underline underline-offset-2',
              'whitespace-nowrap cursor-pointer',
              'hover:text-text-primary hover:bg-transparent active:bg-transparent'
            )}
          >
            전문 보기
          </Button>
        )}
      </div>
    </div>
  )
}
