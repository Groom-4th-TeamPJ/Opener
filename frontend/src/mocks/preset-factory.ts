import { http, HttpResponseResolver } from 'msw'

export default function createPresetHandler(
  method: 'get' | 'post',
  url: string,
  presets: Array<{
    label: string
    resolver: HttpResponseResolver
  }>
) {
  const map = new Map(presets.map((p) => [p.label, p]))

  const use = (label: string) => {
    const p = map.get(label)
    if (!p) throw new Error(`Preset not found: ${label} for ${method.toUpperCase()} ${url}`)
    return http[method](url, p.resolver)
  }

  return {
    url,
    use,
  }
}
