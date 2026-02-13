import { HttpResponse } from 'msw'

export const apiOk = <T>(data: T, message = 'OK', code = 200) =>
  HttpResponse.json({ status: 'success', code, message, data, error: null }, { status: code })

export const apiFail = (message = 'FAIL', code = 400, error: unknown | null = null) =>
  HttpResponse.json({ status: 'fail', code, message, data: null, error }, { status: code })
