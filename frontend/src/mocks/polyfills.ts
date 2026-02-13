import { EventSource } from 'eventsource'

// 테스트 환경에서 API URL 설정 (MSW 핸들러 URL과 일치해야 함)
process.env.NEXT_PUBLIC_API_URL = 'https://opener.ai.kr/api'

// Node.js 환경에서 EventSource 폴리필 설정
// 이 파일은 반드시 MSW 핸들러보다 먼저 import되어야 함
Object.defineProperty(globalThis, 'EventSource', {
  value: EventSource,
  writable: true,
})

// jsdom에서 지원하지 않는 메서드 mock
Element.prototype.scrollIntoView = () => {}
