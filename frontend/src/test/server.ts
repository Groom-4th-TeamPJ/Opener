import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'

export const server = setupServer()

export { http, HttpResponse }
