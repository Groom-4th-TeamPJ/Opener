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
  error: ApiError
}

export type ApiError = {
  field: string | null
  rejectedValue: null
  reason: string
  code: string
}

export type ApiEnvelope<T> = ApiSuccess<T> | ApiFail

export type UiError = {
  code: number
  errorCode: string | null
  message: string
}
