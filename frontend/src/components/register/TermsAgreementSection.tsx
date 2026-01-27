'use client'

import cn from '@/utils/cn'
import { Dispatch, SetStateAction, useState } from 'react'
import { Term, TermKey } from '@/types/auth.types'
import TermItem from '@/components/register/TermItem'
import CircleCheckbox from './CircleCheckBox'
import Image from 'next/image'
import PolicyModal from './PolicyModal'

const POLICY: Record<TermKey, { title: string; url: string }> = {
  service: { title: '서비스 이용약관', url: '/policies/terms.md' },
  privacy: { title: '개인정보처리방침', url: '/policies/privacy.md' },
  age: { title: '만 14세 이상 확인', url: '' },
}

interface TermsAgreementSectionProps {
  agreed: boolean
  terms: Term
  setTerms: Dispatch<SetStateAction<Term>>
  termError: string | null
  setTermError: Dispatch<SetStateAction<string | null>>
}

export default function TermsAgreementSection({
  agreed,
  terms,
  setTerms,
  termError,
  setTermError,
}: TermsAgreementSectionProps) {
  const [openKey, setOpenKey] = useState<TermKey | null>(null)
  const toggleAll = (checked: boolean) => {
    setTerms({ service: checked, privacy: checked, age: checked })
    if (checked) setTermError(null)
  }

  const toggleOne = (key: TermKey, checked: boolean) => {
    setTerms((prev) => {
      const next = { ...prev, [key]: checked }
      return next
    })
    if (checked) {
      setTermError(null)
    }
  }

  return (
    <section className="space-y-2 my-4">
      {/* 전체 동의 + details */}
      <details
        className={cn(
          'group/root rounded-lg bg-background border border-foreground/20 p-4',
          termError ? 'border-red-600' : ''
        )}
      >
        <summary className="flex items-center justify-between gap-3 cursor-pointer select-none">
          <CircleCheckbox checked={agreed} onChange={toggleAll} label="필수 항목 모두 동의" />

          <Image
            src={'icons/chevron_compact-down_gray.svg'}
            alt="자세히 보기"
            width={10}
            height={10}
            className="
            transition-transform duration-200
              group-open/root:rotate-180
          "
          />
        </summary>

        <div className="mt-3 space-y-2 bg-background border-t border-foreground/20 pt-3">
          {Object.entries(terms).map(([key, checked]) => (
            <TermItem
              key={key}
              termKey={key as TermKey}
              checked={checked}
              onChange={(v) => toggleOne(key as TermKey, v)}
              label={POLICY[key as TermKey].title}
              hasContent={Boolean(POLICY[key as TermKey].url)}
              onOpen={setOpenKey}
            />
          ))}
        </div>
      </details>
      {termError && (
        <p id="term-error" className="text-xs text-danger-600" role="alert">
          {termError}
        </p>
      )}

      <PolicyModal
        openKey={openKey}
        onClose={() => setOpenKey(null)}
        policy={POLICY}
        effectiveDate="2026년 1월 28일"
      />
    </section>
  )
}
