export type RegisterMode = 'default' | 'oauth'

export type RegisterFormValues = {
  name: string
  email?: string
  password?: string
}

export type LoginFormValues = {
  email: string
  password: string
}

export type TermKey = 'service' | 'privacy' | 'age'
export type Term = Record<TermKey, boolean>
