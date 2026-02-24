import { memo } from 'react'
import Button from '@/components/common/Button'
import { X } from 'lucide-react'
import SolidCanIcon from '@/components/icons/SolidCanIcon'
import useCanCount from '@/hooks/header/use-can-count'

interface ExamHeaderProps {
  onClose: () => void
}

export default memo(function ExamHeader({ onClose }: ExamHeaderProps) {
  const { data } = useCanCount()
  const canCount = data?.currentCan ?? 0

  return (
    <header className="w-full h-14 bg-white">
      <div className="max-w-6xl mx-auto px-4 md:px-8 h-full flex justify-between items-center">
        <Button
          variant="ghost"
          onClick={onClose}
          className="p-2 flex justify-start items-center gap-2.5 text-text-primary"
        >
          <X className="w-6 h-6" />
        </Button>

        <div className="px-4 py-2 flex justify-start items-center gap-1.5">
          <SolidCanIcon className="text-primary-600" />
          <div className="text-text-primary font-bold leading-6">{canCount}</div>
        </div>
      </div>
    </header>
  )
})
