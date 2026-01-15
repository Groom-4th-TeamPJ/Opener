import Image from 'next/image'

type FAQItemProps = {
  label: string
}

export default function FAQItem({ label }: FAQItemProps) {
  return (
    <div className="flex items-center justify-between px-4 py-3 bg-neutral-50 rounded-lg">
      <span>{label}</span>
      <Image src={'icons/chevron-right.svg'} alt="" width={18} height={18} />
    </div>
  )
}
