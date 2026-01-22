export default function getErrorMessages(errorCode: string | null) {
  const messages: Record<string, string> = {
    A_016: '이미 가입된 사용자입니다.',
    A_017: '인증 세션이 만료되었습니다. 다시 로그인해주세요.',
  }

  if (!errorCode || !messages[errorCode]) {
    return '오류가 발생했습니다. 다시 시도해주세요.'
  }

  return messages[errorCode]
}
