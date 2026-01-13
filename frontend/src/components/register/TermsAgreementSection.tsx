'use client'

import cn from '@/utils/cn'
import { Dispatch, SetStateAction, useState } from 'react'
import { Term, TermKey } from '@/types/auth.types'
import TermItem from '@/components/register/TermItem'
import CircleCheckbox from './CircleCheckBox'
import Image from 'next/image'

const REQUIRED_TERMS: { key: TermKey; label: string; content?: string }[] = [
  {
    key: 'service',
    label: '서비스 이용약관 동의',
    content: `서비스 이용약관 (필수)
  포함 내용
  서비스 제공 범위 (문제 풀이, AI 분석, 기록 저장 등)
  사용자 의무 (부정행위, 계정 공유 금지 등)
  서비스 제한/중단 조건
  책임의 한계 (학습 결과에 대한 법적 책임 X)
  
  [필수] 서비스 이용약관에 동의합니다
  `,
  },
  {
    key: 'privacy',
    label: '개인정보 처리방침 동의',
    content: `개인정보 수집 및 이용 동의 (필수)
  수집 항목 명시
  필수: 이메일(ID), 비밀번호(암호화)
  
  이용 목적
  회원 식별 및 로그인
  학습 기록 저장
  맞춤 문제/AI 분석 제공
  
  보관 기간
  회원 탈퇴 시 즉시 파기 (또는 법적 보관 기간 명시)
  
  [필수] 개인정보 수집 및 이용에 동의합니다
  `,
  },
  {
    key: 'age',
    label: '만 14세 이상입니다',
  },
]

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
          <CircleCheckbox
            checked={agreed}
            onChange={toggleAll}
            label="필수 및 선택 항목 모두 포함 동의"
          />

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
          {Object.entries(terms).map(([key, checked], idx) => (
            <TermItem
              key={key}
              termKey={key as TermKey}
              openKey={openKey}
              setOpenKey={setOpenKey}
              checked={checked}
              onChange={(v) => toggleOne(key as TermKey, v)}
              label={REQUIRED_TERMS[idx].label}
              content={REQUIRED_TERMS[idx].content ?? ''}
            />
          ))}
        </div>
      </details>
      {termError && (
        <p id="term-error" className="text-xs text-danger-600" role="alert">
          {termError}
        </p>
      )}
    </section>
  )
}
