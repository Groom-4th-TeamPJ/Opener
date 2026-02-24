'use client'

import { useEffect, useMemo, useState } from 'react'
import { marked } from 'marked'
import { TermKey } from '@/types/auth.types'
import { Modal, ModalHeader, ModalContent, ModalFooter } from '@/components/common/Modal'
import Button from '@/components/common/Button'

marked.setOptions({ gfm: true, breaks: true })

type PolicyMap = Record<TermKey, { title: string; url: string }>

interface PolicyModalProps {
  openKey: TermKey | null
  onClose: () => void
  policy: PolicyMap
  effectiveDate?: string
}

export default function PolicyModal({ openKey, onClose, policy }: PolicyModalProps) {
  const [mdCache, setMdCache] = useState<Partial<Record<TermKey, string>>>({})

  useEffect(() => {
    if (!openKey) return
    const { url } = policy[openKey]
    if (!url) return
    if (mdCache[openKey]) return

    fetch(url)
      .then((r) => r.text())
      .then((text) => setMdCache((prev) => ({ ...prev, [openKey]: text })))
  }, [openKey, mdCache, policy])

  const html = useMemo(() => {
    if (!openKey) return ''
    const md = mdCache[openKey]
    return md ? marked.parse(md) : ''
  }, [openKey, mdCache])

  const title = openKey ? policy[openKey].title : ''

  return (
    <Modal open={openKey !== null} onClose={onClose} size="3xl" closeOnBackdrop closeOnEscape>
      <ModalHeader closable onClose={onClose}>
        <div className="space-y-1">
          <h2 className="text-lg font-semibold">{title}</h2>
        </div>
      </ModalHeader>

      <ModalContent
        className="py-4 text-sm leading-relaxed policy-content"
        dangerouslySetInnerHTML={{
          __html: html || '<p>불러오는 중…</p>',
        }}
      />

      <ModalFooter>
        <Button
          type="button"
          onClick={onClose}
          className="px-4 py-2 rounded-md bg-neutral-900 text-white hover:bg-neutral-800 active:bg-neutral-700"
        >
          닫기
        </Button>
      </ModalFooter>
    </Modal>
  )
}
