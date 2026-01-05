import { Info } from 'lucide-react'
import cn from '@/utils/cn'
import { Popover, PopoverContent, PopoverTrigger } from './Popover'
import Button from './Button'

interface InfoTooltipProps {
  content: string | React.ReactNode
  side?: 'top' | 'bottom' | 'left' | 'right'
  className?: string // 아이콘 색상이나 위치 조정을 위한 추가 클래스
  disabled?: boolean // 비활성화 상태
}

export function InfoTooltip({
  content,
  side = 'top',
  className,
  disabled = false,
}: InfoTooltipProps) {
  return (
    <Popover>
      <PopoverTrigger asChild disabled={disabled}>
        <Button
          variant="ghost"
          size="sm"
          disabled={disabled}
          className={cn('h-fit w-fit p-0 hover:bg-transparent', className)}
        >
          <Info className="w-5 h-5 text-neutral-600" />
          <span className="sr-only">도움말</span>
        </Button>
      </PopoverTrigger>

      <PopoverContent
        side={side}
        sideOffset={8}
        // 이미지 디자인: 블루 배경 + 흰색 글씨 + 화살표
        className="bg-info-600 text-white border-none shadow-xl py-2.5 w-fit max-w-81"
        arrowClassName="fill-blue-500"
        align="end"
      >
        <div className="text-sm font-medium leading-relaxed break-all whitespace-pre-line">
          {content}
        </div>
      </PopoverContent>
    </Popover>
  )
}
