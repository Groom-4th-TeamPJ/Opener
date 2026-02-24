import { describe, test, expect } from 'vitest'
import getPointVariant from '@/utils/get-point-variant'

describe('getPointVariant', () => {
  test('2점은 success를 반환한다', () => {
    expect(getPointVariant(2)).toBe('success')
  })

  test('3점은 warning을 반환한다', () => {
    expect(getPointVariant(3)).toBe('warning')
  })

  test('4점은 danger를 반환한다', () => {
    expect(getPointVariant(4)).toBe('danger')
  })
})
