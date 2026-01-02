'use client'

import cn from '@/utils/cn'
import { ChevronDown } from 'lucide-react'
import { Dispatch, SetStateAction, useState } from 'react'
import { Term, TermKey } from '@/components/register/RegisterForm'

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
          'group/root rounded-lg bg-background border border-foreground/20 p-3',
          termError ? 'border-red-600' : ''
        )}
      >
        <summary className="flex items-center justify-between gap-3 cursor-pointer select-none">
          <label
            className="flex items-center gap-2 cursor-pointer"
            onClick={(e) => e.stopPropagation()} // summary 토글 방지(체크만)
          >
            <input type="checkbox" checked={agreed} onChange={(e) => toggleAll(e.target.checked)} />
            <span className="text-sm font-medium">[필수] 약관 전체 동의</span>
          </label>

          <ChevronDown
            className={`
              h-4 w-4 text-muted-foreground
              transition-transform duration-200
              group-open/root:rotate-180
            `}
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

function TermItem({
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
        <label
          className="flex items-center gap-2 cursor-pointer"
          onClick={(e) => e.stopPropagation()}
        >
          <input type="checkbox" checked={checked} onChange={(e) => onChange(e.target.checked)} />
          <span className="text-sm">
            {label} <span className="text-red-500">(필수)</span>
          </span>
        </label>

        {content && (
          <ChevronDown
            className="
            h-4 w-4 text-muted-foreground
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
