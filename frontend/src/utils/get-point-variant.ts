export default function getPointVariant(point: number): 'success' | 'warning' | 'danger' {
  if (point === 2) return 'success'
  if (point === 3) return 'warning'
  return 'danger'
}
