import { TermKey } from '@/types/auth.types'
import cn from '@/utils/cn'
import CircleCheckbox from './CircleCheckBox'
import Image from 'next/image'

export default function TermItem({
  termKey,
  openKey,
  setOpenKey,
  checked,
  onChange,
  label,
  content,
}: {
  termKey: TermKey
  openKey: TermKey | null
  setOpenKey: (key: TermKey | null) => void
  checked: boolean
  onChange: (next: boolean) => void
  label: string
  content?: string
}) {
  const isOpen = openKey === termKey

  return (
    <details
      open={isOpen}
      className="group/term rounded-lg bg-background border border-foreground/20 p-3"
      onClick={(e) => {
        e.preventDefault() // 기본 details 토글 방지
        setOpenKey(isOpen ? null : termKey)
      }}
    >
      <summary
        className={cn(
          'flex items-center justify-between gap-3 select-none',
          content ? 'cursor-pointer' : ''
        )}
      >
        {/* 체크 영역 */}
        <CircleCheckbox
          checked={checked}
          onChange={onChange}
          label={
            <>
              {label} <span className="text-danger-600">(필수)</span>
            </>
          }
        />

        {content && (
          <Image
            src={'icons/chevron_compact-down_gray.svg'}
            alt="자세히 보기"
            width={10}
            height={10}
            className="
            transition-transform duration-200
            group-open/term:rotate-180
          "
          />
        )}
      </summary>

      {/* 전문 */}
      {content && (
        <div
          className="mt-2 rounded-md bg-muted/30 
        p-3 text-xs leading-5 
        text-muted-foreground max-h-44 
        overflow-auto whitespace-break-spaces
        "
        >
          {content}
        </div>
      )}
    </details>
  )
}
