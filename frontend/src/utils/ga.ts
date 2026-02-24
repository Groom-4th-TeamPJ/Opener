type GaParams = Record<string, string | number | boolean | undefined | null>

export function gaEvent(name: string, params?: GaParams) {
  if (typeof window === 'undefined') return
  window.gtag?.('event', name, params ?? {})
}
