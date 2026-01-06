export type ApiSuccess<T> = {
  status: 'success'
  code: number // 보통 200
  message: string
  data: T
  error: null
}

export type ApiFail = {
  status: 'fail' | 'error'
  code: number
  message: string
  data: null
  error: unknown
}

export type ApiEnvelope<T> = ApiSuccess<T> | ApiFail
