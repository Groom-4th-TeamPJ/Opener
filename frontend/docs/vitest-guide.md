# Vitest 사용 가이드

## 실행 명령어

```bash
# watch 모드 (파일 변경 시 자동 재실행)
pnpm test

# 1회 실행 후 종료
pnpm test:run
```

## 테스트 파일 위치

Co-located 방식으로 소스 파일 옆에 `__tests__` 폴더를 만들어 테스트 작성

```
src/
  utils/
    get-point-variant.ts
    __tests__/
      get-point-variant.test.ts
  components/
    exam/
      chat/
        AIChatbot.tsx
        __tests__/
          AIChatbot.test.tsx
```

## 기본 문법

### describe

테스트를 그룹으로 묶는다. 중첩 가능.

```ts
describe('계산기', () => {
  describe('덧셈', () => {
    // 덧셈 관련 테스트들
  })

  describe('뺄셈', () => {
    // 뺄셈 관련 테스트들
  })
})
```

### it / test

개별 테스트 케이스를 작성한다. `it`과 `test`는 기능적으로 동일.

**`it`은 BDD(Behavior-Driven Development) 스타일**로, 영어로 작성 시 자연스러운 문장이 된다.

```ts
describe('Calculator', () => {
  it('should add two numbers correctly', () => {})
  it('should return zero when multiplied by zero', () => {})
})
// "it should add two numbers correctly" → 문장처럼 읽힘
```

**`test`는 중립적이고 명시적인 표현**으로, 테스트 케이스라는 사실을 강조

```ts
describe('getPointVariant', () => {
  test('2점은 success를 반환한다', () => {})
  test('3점은 warning을 반환한다', () => {})
})
```

팀 컨벤션에 따라 하나로 통일해서 사용하면 된다.

### expect

값을 검증한다.

```ts
expect(실제값).toBe(기대값)
```

## 테스트 작성 예시

### 유틸리티 함수 테스트

```ts
import { describe, it, expect } from 'vitest'
import getPointVariant from '../get-point-variant'

describe('getPointVariant', () => {
  it('2점은 success를 반환한다', () => {
    expect(getPointVariant(2)).toBe('success')
  })

  it('3점은 warning을 반환한다', () => {
    expect(getPointVariant(3)).toBe('warning')
  })

  it('4점은 danger를 반환한다', () => {
    expect(getPointVariant(4)).toBe('danger')
  })
})
```

### React 컴포넌트 테스트

```tsx
import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import MyComponent from '../MyComponent'

describe('MyComponent', () => {
  it('텍스트가 렌더링된다', () => {
    render(<MyComponent />)
    expect(screen.getByText('Hello')).toBeInTheDocument()
  })
})
```

## 주요 Matcher

```ts
// 값 비교
expect(value).toBe(expected) // 정확히 일치 (===)
expect(value).toEqual(expected) // 깊은 비교 (객체/배열)
expect(value).toBeTruthy() // truthy 값
expect(value).toBeFalsy() // falsy 값
expect(value).toBeNull() // null
expect(value).toBeUndefined() // undefined

// 숫자
expect(value).toBeGreaterThan(3) // > 3
expect(value).toBeLessThan(5) // < 5

// 문자열
expect(str).toContain('hello') // 문자열 포함
expect(str).toMatch(/정규식/) // 정규식 매칭

// 배열
expect(arr).toContain(item) // 배열에 포함
expect(arr).toHaveLength(3) // 길이

// DOM (@testing-library/jest-dom)
expect(element).toBeInTheDocument() // DOM에 존재
expect(element).toHaveClass('foo') // 클래스 보유
expect(element).toBeVisible() // 화면에 보임
expect(element).toHaveTextContent('text') // 텍스트 포함
```

## 특정 테스트만 실행

```bash
# 파일명으로 필터
pnpm test get-point-variant
```

```ts
// 코드에서 특정 테스트만 실행
describe.only('이것만 실행', () => { ... })
it.only('이것만 실행', () => { ... })

// 특정 테스트 스킵
describe.skip('스킵', () => { ... })
it.skip('스킵', () => { ... })
```

## 디버깅

```ts
import { render, screen } from '@testing-library/react'

render(<MyComponent />)
screen.debug()                        // 전체 DOM 출력
screen.debug(screen.getByRole('button')) // 특정 요소만 출력
```
